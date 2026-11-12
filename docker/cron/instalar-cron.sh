#!/usr/bin/env bash
# Instala las tareas programadas de DonaTrack en la VM (Ubuntu/Debian). Correr una vez, con sudo, desde
# el repo clonado en la VM:
#
#   sudo SERVICIOS_IP=10.0.0.4 bash docker/cron/instalar-cron.sh   # VM de cron separada (deploy/README.md)
#   sudo bash docker/cron/instalar-cron.sh                         # todo en una VM, con docker exec
#
# Con SERVICIOS_IP (la IP privada de la VM de servicios) las tareas se disparan con curl por la red privada,
# contra el balanceador de esa VM: deja las URLs y la SERVICE_API_KEY (la pide si no esta en el entorno) en
# /etc/donatrack/cron.env, que solo puede leer el usuario que corre las tareas. Esta VM no necesita Docker.
#
# Deja la VM en hora de Argentina, crea /etc/cron.d/donatrack (a partir de donatrack.cron), la carpeta
# de logs /var/log/donatrack y su rotacion semanal. Se puede volver a correr sin problema.
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
    echo "Correlo con sudo: sudo bash $0" >&2
    exit 1
fi

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_REPO="$(cd "$DIR_SCRIPT/../.." && pwd)"
# el usuario que corre las tareas es el duenio del repo (en Azure, normalmente azureuser)
USUARIO="${SUDO_USER:-$(stat -c '%U' "$DIR_REPO")}"
ZONA="America/Argentina/Buenos_Aires"
SERVICIOS_IP="${SERVICIOS_IP:-}"
CRON_ENV=/etc/donatrack/cron.env

command -v flock >/dev/null || { echo "Falta flock: sudo apt-get install -y util-linux" >&2; exit 1; }

if [ -n "$SERVICIOS_IP" ]; then
    command -v curl >/dev/null || { echo "Falta curl: sudo apt-get install -y curl" >&2; exit 1; }
    if [ -z "${SERVICE_API_KEY:-}" ]; then
        read -rsp "SERVICE_API_KEY (la misma del .env de la VM de servicios): " SERVICE_API_KEY
        echo
    fi
    [ -n "$SERVICE_API_KEY" ] || { echo "Falta la SERVICE_API_KEY" >&2; exit 1; }

    mkdir -p "$(dirname "$CRON_ENV")"
    # printf %q deja la clave lista para que la lea el shell, aunque tenga caracteres especiales
    {
        echo "# Generado por instalar-cron.sh: tareas por la red privada, contra el balanceador"
        echo "CRON_CONTENEDOR="
        echo "DONACIONES_URL=http://$SERVICIOS_IP:8080/api"
        echo "LOGISTICA_URL=http://$SERVICIOS_IP:8081/api"
        printf 'SERVICE_API_KEY=%q\n' "$SERVICE_API_KEY"
    } > "$CRON_ENV"
    chown "$USUARIO" "$CRON_ENV"
    chmod 600 "$CRON_ENV"
    echo "Configuracion en $CRON_ENV (servicios en $SERVICIOS_IP)"

    echo "Probando la conexion con el balanceador..."
    if curl -sS -o /dev/null --max-time 10 "http://$SERVICIOS_IP:8080/"; then
        echo "  OK: responde http://$SERVICIOS_IP:8080"
    else
        echo "  AVISO: no responde http://$SERVICIOS_IP:8080 (revisar el NSG y que los contenedores esten arriba)" >&2
    fi
else
    rm -f "$CRON_ENV"
    command -v docker >/dev/null || { echo "Falta docker" >&2; exit 1; }
    # el script llama a la API con "docker exec": el usuario tiene que poder usar docker sin sudo
    if ! id -nG "$USUARIO" | grep -qw docker; then
        echo "El usuario $USUARIO no esta en el grupo docker: sudo usermod -aG docker $USUARIO" >&2
        exit 1
    fi
fi

echo "Zona horaria de la VM: $ZONA"
timedatectl set-timezone "$ZONA"

chmod +x "$DIR_SCRIPT/donatrack-cron.sh"

mkdir -p /var/log/donatrack
chown "$USUARIO" /var/log/donatrack

sed -e "s|__USUARIO__|$USUARIO|g" -e "s|__DIR__|$DIR_REPO|g" "$DIR_SCRIPT/donatrack.cron" \
    > /etc/cron.d/donatrack
chmod 644 /etc/cron.d/donatrack

cat > /etc/logrotate.d/donatrack <<'EOF'
/var/log/donatrack/*.log {
    weekly
    rotate 8
    compress
    missingok
    notifempty
    copytruncate
}
EOF

# cron toma los cambios de /etc/cron.d solo, pero tras cambiar la zona horaria hay que reiniciarlo
systemctl restart cron

echo "Listo. Tareas instaladas en /etc/cron.d/donatrack (usuario $USUARIO, repo $DIR_REPO):"
grep -E '^[0-9]' /etc/cron.d/donatrack
echo "Log: /var/log/donatrack/cron.log"
