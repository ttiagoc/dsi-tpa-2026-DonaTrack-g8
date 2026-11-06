#!/usr/bin/env bash
# Instala las tareas programadas de DonaTrack en la VM (Ubuntu/Debian). Correr una vez, con sudo, desde
# el repo clonado en la VM:
#
#   sudo bash docker/cron/instalar-cron.sh
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

command -v curl >/dev/null || { echo "Falta curl: sudo apt-get install -y curl" >&2; exit 1; }
command -v flock >/dev/null || { echo "Falta flock: sudo apt-get install -y util-linux" >&2; exit 1; }

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
