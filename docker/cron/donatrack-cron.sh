#!/usr/bin/env bash
# Dispara una de las tareas programadas de DonaTrack llamando a la API con la clave de servicio
# (rol SISTEMA). Lo ejecuta el cron de la VM; ver /etc/cron.d/donatrack (lo instala instalar-cron.sh).
#
#   donatrack-cron.sh matchmaking   -> algoritmos de asignacion de las donaciones en deposito
#   donatrack-cron.sh rutas         -> planificacion de las rutas del dia siguiente
#   donatrack-cron.sh inactividad   -> aviso a los donantes que cumplen 20 dias sin donar
#
# Configuracion (variables de entorno, todas opcionales):
#   DONATRACK_DIR    carpeta del repo; de su .env se lee SERVICE_API_KEY (por defecto, la carpeta del repo
#                    que contiene este script)
#   SERVICE_API_KEY  clave compartida entre servicios; si no esta, se lee del .env o se usa la de compose
#   DONACIONES_URL   por defecto http://localhost:8080/api (puerto publicado por docker compose)
#   LOGISTICA_URL    por defecto http://localhost:8081/api
#   CRON_TIMEOUT     segundos maximos de cada llamada, por defecto 900
set -uo pipefail

TAREA="${1:-}"
DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DONATRACK_DIR="${DONATRACK_DIR:-$(cd "$DIR_SCRIPT/../.." && pwd)}"
DONACIONES_URL="${DONACIONES_URL:-http://localhost:8080/api}"
LOGISTICA_URL="${LOGISTICA_URL:-http://localhost:8081/api}"
CRON_TIMEOUT="${CRON_TIMEOUT:-900}"
INTENTOS=3
ESPERA_ENTRE_INTENTOS=60

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

# La misma clave que usan los contenedores: primero el entorno, despues el .env del repo, despues el
# valor por defecto de docker-compose.yml.
if [ -z "${SERVICE_API_KEY:-}" ] && [ -f "$DONATRACK_DIR/.env" ]; then
    SERVICE_API_KEY="$(grep -E '^[[:space:]]*SERVICE_API_KEY=' "$DONATRACK_DIR/.env" | tail -n 1 \
        | cut -d= -f2- | tr -d '\r' | sed -e 's/^["'\'']//' -e 's/["'\'']$//')"
fi
SERVICE_API_KEY="${SERVICE_API_KEY:-donatrack-clave-servicios-local}"

for comando in curl flock; do
    if ! command -v "$comando" >/dev/null 2>&1; then
        log "ERROR: falta el comando $comando"
        exit 1
    fi
done

# Una sola ejecucion por tarea a la vez: si la anterior sigue corriendo, esta no arranca.
CANDADO="/tmp/donatrack-cron-$TAREA.lock"
exec 9>"$CANDADO"
if ! flock -n 9; then
    log "La ejecucion anterior sigue en curso: se omite esta."
    exit 0
fi

RESPUESTA="$(mktemp)"
trap 'rm -f "$RESPUESTA"' EXIT

log "Inicio: POST $URL"
for intento in $(seq 1 "$INTENTOS"); do
    INICIO=$(date +%s)
    CODIGO="$(curl -sS -o "$RESPUESTA" -w '%{http_code}' --max-time "$CRON_TIMEOUT" \
        -X POST -H "X-Api-Key: $SERVICE_API_KEY" "$URL")"
    ESTADO_CURL=$?
    DURACION=$(( $(date +%s) - INICIO ))

    if [ "$ESTADO_CURL" -eq 0 ] && [ "$CODIGO" -ge 200 ] && [ "$CODIGO" -lt 300 ]; then
        log "OK (HTTP $CODIGO, ${DURACION}s)"
        exit 0
    fi

    if [ "$ESTADO_CURL" -ne 0 ]; then
        # sin conexion (servicio reiniciandose) o timeout: se reintenta
        log "Intento $intento/$INTENTOS sin respuesta (curl $ESTADO_CURL, ${DURACION}s)"
    else
        # la API respondio con error: reintentar no lo arregla (clave incorrecta, error de negocio, ...)
        log "ERROR HTTP $CODIGO (${DURACION}s): $(head -c 500 "$RESPUESTA")"
        exit 1
    fi

    if [ "$intento" -lt "$INTENTOS" ]; then
        sleep "$ESPERA_ENTRE_INTENTOS"
    fi
done

log "ERROR: no se pudo completar despues de $INTENTOS intentos"
exit 1
