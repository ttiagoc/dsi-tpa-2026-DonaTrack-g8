# Despliegue en Azure (varias VMs)

El sistema se reparte en cuatro piezas, todas dentro de la misma red virtual (VNet) de Azure:

| Pieza | Qué es | Qué corre |
|---|---|---|
| **Base de datos** | Azure Database for PostgreSQL (Flexible Server) o una VM con PostgreSQL | Las bases `donaciones`, `logistica` y `notificaciones` |
| **VM de servicios** | La VM que ya existía (B2als v2: 2 vCPU, 4 GB) | Caddy (balanceador y HTTPS) y **dos instancias** de cada servicio y del web-service ([`servicios/`](servicios/)) |
| **VM de cron** | Una VM chica (B2ats v2) sin IP pública | El cron que dispara las tareas programadas ([`../docker/cron/`](../docker/cron/)) |
| **Almacenamiento de fotos** | Un file share de Azure Files | Las fotos de recepción, compartidas por las dos instancias del web-service |

```
Internet ──HTTPS──► VM de servicios (IP pública, 80/443)
                      Caddy ──┬─► web-1 / web-2 (sesiones pegajosas)
                              ├─► :8080 donaciones-1 / donaciones-2
                              ├─► :8081 logistica-1 / logistica-2
                              └─► :8082 notificaciones-1 / notificaciones-2
                      /mnt/donatrack-fotos ◄── Azure Files (SMB)
                               │
                 red privada ──┼───────────────────────────┐
                               ▼                           ▼
                  PostgreSQL (3 bases)        VM de cron ──curl──► <IP privada>:8080 / :8081
```

Para desarrollo local no cambia nada: se sigue usando el `docker-compose.yml` de la raíz (ver el [Readme](../Readme.md)).

## Cómo funciona

- **Balanceador:** Caddy reparte las requests entre las dos instancias de cada servicio. Si una no responde, la saca
  de la rotación por 30 s y reintenta en la otra, así que el sistema sigue andando con una instancia caída. Ver
  [`servicios/Caddyfile`](servicios/Caddyfile).
- **Sesiones pegajosas en el sitio:** la sesión del web-service vive en la memoria de la instancia que logueó al
  usuario, así que Caddy le pone una cookie (`donatrack_instancia`) y lo manda siempre a la misma. Si esa instancia
  se cae, el usuario pasa a la otra y tiene que volver a ingresar.
- **APIs sin estado:** cada request a las APIs trae su JWT o su `X-Api-Key`, así que cualquier instancia la puede
  atender. El tráfico HTTP entre servicios también pasa por Caddy (`http://caddy:8080/api`, ...). El gRPC de
  logística a donaciones va directo al alias `donaciones-service`, que Docker resuelve a las dos instancias.
- **Fotos compartidas:** el web-service las sigue guardando en `FOTOS_DIR`, pero esa carpeta es un file share de
  Azure Files montado en la VM. Las dos instancias ven las mismas fotos, y las fotos no se pierden si se rehace la VM.
- **Tareas programadas una sola vez:** la VM de cron llama por la red privada al balanceador
  (`http://<IP privada de servicios>:8080/api/...`) y Caddy se lo pasa a **una** instancia. Los servicios no
  programan nada por su cuenta, así que tener dos instancias no duplica las tareas.
- **Qué queda expuesto:** desde Internet solo entra el 80/443 de la VM de servicios (y el 22 para SSH). Los
  puertos 8080/8081 de Caddy solo se abren a la VNet en el NSG, y Caddy además rechaza cualquier IP que no sea
  privada. La base solo acepta conexiones desde la VNet.

## Red

Todo está en el grupo de recursos `DONATRACK`, región `chilecentral`, VNet `vnet-chilecentral-1` (`172.16.0.0/16`):

| Subred | Rango | Qué tiene |
|---|---|---|
| `snet-chilecentral-1` | `172.16.0.0/24` | VM de servicios `donatrack-vm` (`172.16.0.4`, B2als v2) |
| `snet-db` | `172.16.1.0/24` | PostgreSQL `donatrack-g8-db` (subred delegada a `Microsoft.DBforPostgreSQL/flexibleServers`) |
| `snet-cron` | `172.16.2.0/24` | VM de cron `donatrack-cron` (`172.16.2.4`, B2ats v2, sin IP pública) |

| VM | Entrada permitida |
|---|---|
| Servicios | 80, 443 desde Internet. 22 para SSH. 8080 y 8081 solo desde la VNet (la regla por defecto `AllowVnetInBound`; ninguna regla los abre a Internet) |
| Cron | Solo desde la VNet: se entra por SSH saltando por la VM de servicios |
| Base de datos | Sin acceso público: solo desde la VNet |

Los comandos de abajo se corren en Azure Cloud Shell (Bash). Si la suscripción nunca usó PostgreSQL o Storage,
primero hay que registrarlos (una sola vez):

```bash
az provider register --namespace Microsoft.DBforPostgreSQL --wait
az provider register --namespace Microsoft.Storage --wait
```

## 1. Base de datos

**Azure Database for PostgreSQL Flexible Server** B1ms (entra en el plan gratuito de 12 meses), con acceso privado.
La subred y la zona DNS privada se crean antes: si se le pide a `flexible-server create` que cree la zona, falla con
`PrivateDnsZoneNameNotValid` sea cual sea el nombre.

```bash
az network vnet subnet create -g DONATRACK --vnet-name vnet-chilecentral-1 -n snet-db \
  --address-prefixes 172.16.1.0/24 --delegations Microsoft.DBforPostgreSQL/flexibleServers
az network private-dns zone create -g DONATRACK -n donatrack.private.postgres.database.azure.com
az network private-dns link vnet create -g DONATRACK -z donatrack.private.postgres.database.azure.com \
  -n donatrack-db-link --virtual-network vnet-chilecentral-1 --registration-enabled false

ZONA_ID=$(az network private-dns zone show -g DONATRACK -n donatrack.private.postgres.database.azure.com --query id -o tsv)
SUBNET_ID=$(az network vnet subnet show -g DONATRACK --vnet-name vnet-chilecentral-1 -n snet-db --query id -o tsv)
read -rsp "Contraseña de la base: " DB_PASS; echo
az postgres flexible-server create -g DONATRACK -n donatrack-g8-db -l chilecentral \
  --tier Burstable --sku-name Standard_B1ms --storage-size 32 --version 16 \
  --admin-user donatrack --admin-password "$DB_PASS" \
  --subnet "$SUBNET_ID" --private-dns-zone "$ZONA_ID" \
  --zonal-resiliency Disabled --backup-retention 7 --yes

for base in donaciones logistica notificaciones; do
  az postgres flexible-server db create -g DONATRACK --server-name donatrack-g8-db --name $base
done
```

La contraseña va sin símbolos (`$` y las comillas se interpretan en el `.env` de compose). Las tablas las crea
Hibernate solo (`hbm2ddl=update`). En el `.env`: `DB_HOST=donatrack-g8-db.postgres.database.azure.com`,
`DB_USUARIO=donatrack`, `DB_SSLMODE=require`.

Para pasar los datos de la VM de una sola máquina (contenedor `donatrack-postgres`), desde esa VM y con la web
frenada para que nadie escriba mientras tanto:

```bash
docker stop donatrack-web
read -rsp "Contraseña de la base: " PGPASSWORD; export PGPASSWORD; echo
for base in donaciones logistica notificaciones; do
  docker exec donatrack-postgres pg_dump -U postgres --no-owner --no-acl "$base" \
  | docker run --rm -i -e PGPASSWORD -e PGSSLMODE=require postgres:16-alpine \
      psql -h donatrack-g8-db.postgres.database.azure.com -U donatrack -d "$base" -v ON_ERROR_STOP=1 -q
done
```

## 2. Fotos (Azure Files)

Cuenta de almacenamiento `donatrackg8fotos` con un file share `fotos`:

```bash
az storage account create -g DONATRACK -n donatrackg8fotos -l chilecentral --sku Standard_LRS --kind StorageV2 \
  --min-tls-version TLS1_2 --allow-blob-public-access false
az storage share-rm create -g DONATRACK --storage-account donatrackg8fotos -n fotos --quota 5
az storage account keys list -g DONATRACK -n donatrackg8fotos --query '[0].value' -o tsv    # la clave para montarlo
```

En la VM de servicios:

```bash
sudo CUENTA=<cuenta> SHARE=fotos bash deploy/servicios/montar-fotos.sh
```

Pide la clave de la cuenta y deja el montaje en `/etc/fstab`, en `/mnt/donatrack-fotos`. Para copiar las fotos que
ya estaban en el volumen de Docker de la VM actual:

```bash
docker run --rm -v donatrack_fotos-data:/de -v /mnt/donatrack-fotos:/a alpine cp -r /de/. /a/
```

## 3. VM de servicios

Con el repo clonado en la VM:

```bash
cp deploy/servicios/.env.example deploy/servicios/.env    # y completarlo
docker compose -f deploy/servicios/docker-compose.yml --env-file deploy/servicios/.env up -d --build
```

Si en la VM estaba levantado el compose de la raíz, primero hay que bajarlo (`docker compose --profile full down`)
para liberar los puertos 80/443. Sin `-v`, así se conservan los volúmenes hasta terminar de migrar.

Comandos útiles:

```bash
docker compose -f deploy/servicios/docker-compose.yml ps
docker compose -f deploy/servicios/docker-compose.yml logs -f caddy donaciones-1 donaciones-2
docker stop donatrack-web-1        # probar que el sitio sigue andando con una sola instancia
```

## 4. VM de cron

Sin IP pública y con la misma clave SSH que la VM de servicios. `Standard_B1s` no tenía capacidad en Chile Central,
así que es una `Standard_B2ats_v2` (también entra en las horas gratis):

```bash
az network vnet subnet create -g DONATRACK --vnet-name vnet-chilecentral-1 -n snet-cron --address-prefixes 172.16.2.0/24
CLAVE_SSH=$(az vm show -g DONATRACK -n donatrack-vm --query 'osProfile.linuxConfiguration.ssh.publicKeys[0].keyData' -o tsv)
az vm create -g DONATRACK -n donatrack-cron -l chilecentral --image Ubuntu2404 --size Standard_B2ats_v2 \
  --vnet-name vnet-chilecentral-1 --subnet snet-cron --public-ip-address "" --nsg-rule NONE \
  --admin-username azureuser --ssh-key-values "$CLAVE_SSH" --os-disk-size-gb 64 --storage-sku Premium_LRS
```

Para entrar, en el `~/.ssh/config` de la compu (con el host `donatrack` de la VM de servicios ya configurado):

```
Host donatrack-cron
    HostName 172.16.2.4
    User azureuser
    IdentityFile <la misma clave .pem>
    ProxyJump donatrack
```

La VM no tiene salida a Internet ni Docker, y no los necesita: la imagen de Ubuntu ya trae `curl` y `flock`. No se
clona el repo, se copian los scripts y se instala el cron:

```bash
ssh donatrack-cron "mkdir -p ~/donatrack/docker"
scp -r docker/cron donatrack-cron:~/donatrack/docker/
ssh donatrack-cron "sudo SERVICIOS_IP=172.16.0.4 bash ~/donatrack/docker/cron/instalar-cron.sh"
```

Pide la `SERVICE_API_KEY` (la misma del `.env` de servicios) y la guarda en `/etc/donatrack/cron.env`. Los
horarios, el log y cómo probar cada tarea a mano están en el [Readme](../Readme.md#tareas-programadas-cron-de-la-vm).
