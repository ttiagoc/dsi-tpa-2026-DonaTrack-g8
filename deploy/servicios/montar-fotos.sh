#!/usr/bin/env bash
# Monta en la VM de servicios el file share de Azure Files donde se guardan las fotos de recepcion, para que
# las dos instancias del web-service lean y escriban en el mismo lugar. Correr una vez, con sudo:
#
#   sudo CUENTA=donatrackfotos SHARE=fotos bash deploy/servicios/montar-fotos.sh
#
# Pide la clave de la cuenta de almacenamiento (portal: cuenta > Claves de acceso) si no esta en CLAVE.
# Deja el montaje en /etc/fstab, asi vuelve solo cuando la VM se reinicia. Se puede volver a correr.
#
#   CUENTA  nombre de la cuenta de almacenamiento (obligatorio)
#   SHARE   nombre del file share, por defecto fotos
#   PUNTO   carpeta de la VM, por defecto /mnt/donatrack-fotos (la misma que FOTOS_DIR_VM del .env)
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
    echo "Correlo con sudo: sudo CUENTA=... bash $0" >&2
    exit 1
fi

CUENTA="${CUENTA:?Falta CUENTA: el nombre de la cuenta de almacenamiento}"
SHARE="${SHARE:-fotos}"
PUNTO="${PUNTO:-/mnt/donatrack-fotos}"
CREDENCIALES="/etc/smbcredentials/$CUENTA.cred"
ORIGEN="//$CUENTA.file.core.windows.net/$SHARE"

if [ -z "${CLAVE:-}" ]; then
    read -rsp "Clave de la cuenta $CUENTA: " CLAVE
    echo
fi

command -v mount.cifs >/dev/null || { apt-get update && apt-get install -y cifs-utils; }

mkdir -p /etc/smbcredentials "$PUNTO"
printf 'username=%s\npassword=%s\n' "$CUENTA" "$CLAVE" > "$CREDENCIALES"
chmod 600 "$CREDENCIALES"

# nofail: si el share no esta, la VM arranca igual. Los permisos 0777 son para el usuario de los contenedores.
OPCIONES="nofail,_netdev,vers=3.1.1,credentials=$CREDENCIALES,dir_mode=0777,file_mode=0777,serverino,nosharesock,actimeo=30,mfsymlinks"
LINEA="$ORIGEN $PUNTO cifs $OPCIONES 0 0"

# reemplaza la linea anterior de este punto de montaje, si la habia
sed -i "\| $PUNTO cifs |d" /etc/fstab
echo "$LINEA" >> /etc/fstab

mountpoint -q "$PUNTO" && umount "$PUNTO"
mount "$PUNTO"

echo "Listo: $ORIGEN montado en $PUNTO"
df -h "$PUNTO"
