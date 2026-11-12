#!/usr/bin/env bash
# Dispara una de las tareas programadas de DonaTrack llamando a la API con la clave de servicio
# (rol SISTEMA). Lo ejecuta el cron de la VM; ver /etc/cron.d/donatrack (lo instala instalar-cron.sh).
#
#   donatrack-cron.sh matchmaking   -> algoritmos de asignacion de las donaciones en deposito
#   donatrack-cron.sh rutas         -> planificacion de las rutas del dia siguiente
#   donatrack-cron.sh inactividad   -> aviso a los donantes que cumplen 20 dias sin donar
#
# Dos formas de llamar:
# - VM de cron separada (deploy/README.md): curl por la red privada al balanceador de la VM de servicios
#   (http://<IP privada>:8080/api y :8081/api). instalar-cron.sh deja las URLs y la clave en
#   /etc/donatrack/cron.env, que este script lee al arrancar.
# - Todo en una VM (docker-compose.yml de la raiz): los servicios no publican puertos (solo Caddy, en
#   80/443), asi que la llamada se hace desde adentro del contenedor de donaciones con "docker exec", por la
#   red de compose y con la SERVICE_API_KEY que ese contenedor ya tiene en su entorno.
#
# Configuracion (variables de entorno o /etc/donatrack/cron.env, todas opcionales):
#   CRON_ENV         archivo de configuracion, por defecto /etc/donatrack/cron.env (si existe)
#   CRON_CONTENEDOR  contenedor desde el que se llama, por defecto donatrack-donaciones. Vacio: se llama
#                    con el curl de la maquina, a los puertos publicados (por ejemplo, en una compu local)
#   DONACIONES_URL   por defecto http://donaciones-service:8080/api (o http://localhost:8080/api sin contenedor)
#   LOGISTICA_URL    por defecto http://logistica-service:8081/api (o http://localhost:8081/api sin contenedor)
#   SERVICE_API_KEY  solo sin contenedor; si no esta, se lee del .env del repo o se usa la de compose
#   DONATRACK_DIR    carpeta del repo, para ese .env (por defecto, la que contiene este script)
#   CRON_TIMEOUT     segundos maximos de cada llamada, por defecto 900
set -uo pipefail

TAREA="${1:-}"
DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DONATRACK_DIR="${DONATRACK_DIR:-$(cd "$DIR_SCRIPT/../.." && pwd)}"
CRON_ENV="${CRON_ENV:-/etc/donatrack/cron.env}"
if [ -r "$CRON_ENV" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$CRON_ENV"
    set +a
fi
CRON_CONTENEDOR="${CRON_CONTENEDOR-donatrack-donaciones}"
CRON_TIMEOUT="${CRON_TIMEOUT:-900}"
INTENTOS=3
ESPERA_ENTRE_INTENTOS=60

if [ -n "$CRON_CONTENEDOR" ]; then
    DONACIONES_URL="${DONACIONES_URL:-http://donaciones-service:8080/api}"
    LOGISTICA_URL="${LOGISTICA_URL:-http://logistica-service:8081/api}"
    REQUERIDOS="docker flock"
else
    DONACIONES_URL="${DONACIONES_URL:-http://localhost:8080/api}"
    LOGISTICA_URL="${LOGISTICA_URL:-http://localhost:8081/api}"
    REQUERIDOS="curl flock"
fi

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S %z') [$TAREA] $*"
}

case "$TAREA" in
    matchmaking) URL="$DONACIONES_URL/matchmaking/ejecuciones" ;;
    rutas)       URL="$LOGISTICA_URL/rutas/planificaciones" ;;
    inactividad) URL="$DONACIONES_URL/donantes/inactividad/verificaciones-diarias" ;;
    *)
        echo "Uso: $0 matchmaking|rutas|inactividad" >&2
        exit 2
        ;;
esac

for comando in $REQUERIDOS; do
    if ! command -v "$comando" >/dev/null 2>&1; then
        log "ERROR: falta el comando $comando"
        exit 1
    fi
done

if [ -z "$CRON_CONTENEDOR" ]; then
    # Sin contenedor: la clave sale del entorno, despues del .env del repo, despues del valor por
    # defecto de docker-compose.yml.
    if [ -z "${SERVICE_API_KEY:-}" ] && [ -f "$DONATRACK_DIR/.env" ]; then
        SERVICE_API_KEY="$(grep -E '^[[:space:]]*SERVICE_API_KEY=' "$DONATRACK_DIR/.env" | tail -n 1 \
            | cut -d= -f2- | tr -d '\r' | sed -e 's/^["'\'']//' -e 's/["'\'']$//')"
    fi
    export SERVICE_API_KEY="${SERVICE_API_KEY:-donatrack-clave-servicios-local}"
fi

# Hace el POST e imprime el cuerpo de la respuesta y, en la ultima linea, el codigo HTTP.
# La clave se expande adentro del sh (del contenedor o local), no en la linea de comando del host.
llamar() {
    local comando='curl -sS -w "\n%{http_code}" --max-time "$1" -X POST -H "X-Api-Key: $SERVICE_API_KEY" "$2"'
    if [ -n "$CRON_CONTENEDOR" ]; then
        docker exec "$CRON_CONTENEDOR" sh -c "$comando" llamar "$CRON_TIMEOUT" "$URL"
    else
        sh -c "$comando" llamar "$CRON_TIMEOUT" "$URL"
    fi
}

# Una sola ejecucion por tarea a la vez: si la anterior sigue corriendo, esta no arranca.
CANDADO="/tmp/donatrack-cron-$TAREA.lock"
exec 9>"$CANDADO"
if ! flock -n 9; then
    log "La ejecucion anterior sigue en curso: se omite esta."
    exit 0
fi

log "Inicio: POST $URL${CRON_CONTENEDOR:+ (desde $CRON_CONTENEDOR)}"
for intento in $(seq 1 "$INTENTOS"); do
    INICIO=$(date +%s)
    SALIDA="$(llamar 2>&1)"
    ESTADO=$?
    DURACION=$(( $(date +%s) - INICIO ))
    CODIGO="${SALIDA##*$'\n'}"
    CUERPO="${SALIDA%$'\n'*}"
    [ "$CUERPO" = "$SALIDA" ] && CUERPO=""

    if [ "$ESTADO" -eq 0 ] && [[ "$CODIGO" =~ ^2[0-9][0-9]$ ]]; then
        log "OK (HTTP $CODIGO, ${DURACION}s)"
        exit 0
    fi

    if [ "$ESTADO" -eq 0 ] && [[ "$CODIGO" =~ ^[0-9]{3}$ ]] && [ "$CODIGO" != "000" ]; then
        # la API respondio con error: reintentar no lo arregla (clave incorrecta, error de negocio, ...)
        log "ERROR HTTP $CODIGO (${DURACION}s): $(printf '%s' "$CUERPO" | head -c 500)"
        exit 1
    fi

    # sin conexion (servicio o contenedor reiniciandose) o timeout: se reintenta
    DETALLE="$(printf '%s' "$SALIDA" | tr '\n' ' ' | sed -e 's/^ *000 *//' -e 's/ *000 *$//' | head -c 300)"
    log "Intento $intento/$INTENTOS sin respuesta (salida $ESTADO, ${DURACION}s): $DETALLE"
    if [ "$intento" -lt "$INTENTOS" ]; then
        sleep "$ESPERA_ENTRE_INTENTOS"
    fi
done

log "ERROR: no se pudo completar despues de $INTENTOS intentos"
exit 1
