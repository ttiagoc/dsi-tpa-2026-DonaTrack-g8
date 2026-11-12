# Despliegue en Azure (varias VMs)

El sistema se reparte en cuatro piezas, todas dentro de la misma red virtual (VNet) de Azure:

| Pieza | Qué es | Qué corre |
|---|---|---|
| **Base de datos** | Azure Database for PostgreSQL (Flexible Server) o una VM con PostgreSQL | Las bases `donaciones`, `logistica` y `notificaciones` |
| **VM de servicios** | La VM que ya existía (B2as v2: 2 vCPU, 8 GB) | Caddy (balanceador y HTTPS) y **dos instancias** de cada servicio y del web-service ([`servicios/`](servicios/)) |
| **VM de cron** | Una VM chica (B1s) sin IP pública | El cron que dispara las tareas programadas ([`../docker/cron/`](../docker/cron/)) |
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

## Red (NSG)

| VM | Entrada permitida |
|---|---|
| Servicios | 80, 443 desde Internet. 22 para SSH. 8080 y 8081 solo desde la subred de la VM de cron (o `VirtualNetwork`) |
| Cron | 22 para SSH (o por la VM de servicios como salto, sin IP pública) |
| Base de datos | 5432 solo desde la VNet (acceso privado del Flexible Server, o el NSG si es una VM) |

## 1. Base de datos

Con **Azure Database for PostgreSQL Flexible Server** (B1ms, entra en el plan gratuito de 12 meses), con acceso
privado en la VNet. Crear las tres bases:

```bash
psql "host=<servidor>.postgres.database.azure.com user=<usuario> dbname=postgres sslmode=require" \
  -c "CREATE DATABASE donaciones" -c "CREATE DATABASE logistica" -c "CREATE DATABASE notificaciones"
```

Las tablas las crea Hibernate solo (`hbm2ddl=update`). Con una **VM con PostgreSQL** es lo mismo: crear las tres
bases y en el `.env` poner su IP privada en `DB_HOST` y `DB_SSLMODE=disable` si no tiene TLS.

Para pasar los datos que ya están en la VM actual (contenedor `donatrack-postgres`):

```bash
for base in donaciones logistica notificaciones; do
  docker exec donatrack-postgres pg_dump -U postgres --no-owner "$base" > "$base.sql"
  psql "host=<servidor>.postgres.database.azure.com user=<usuario> dbname=$base sslmode=require" -f "$base.sql"
done
```

## 2. Fotos (Azure Files)

Crear una cuenta de almacenamiento y un file share `fotos`. En la VM de servicios:

```bash
sudo CUENTA=<cuenta> SHARE=fotos bash deploy/servicios/montar-fotos.sh
```

Pide la clave de la cuenta y deja el montaje en `/etc/fstab`, en `/mnt/donatrack-fotos`. Para copiar las fotos que
ya estaban en el volumen de Docker de la VM actual:

```bash
docker run --rm -v donatrack_fotos-data:/de -v /mnt/donatrack-fotos:/a alpine cp -a /de/. /a/
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

Una VM con Ubuntu, con `curl` (no necesita Docker) y el repo clonado:

```bash
sudo SERVICIOS_IP=<IP privada de la VM de servicios> bash docker/cron/instalar-cron.sh
```

Pide la `SERVICE_API_KEY` (la misma del `.env` de servicios) y la guarda en `/etc/donatrack/cron.env`. Los
horarios, el log y cómo probar cada tarea a mano están en el [Readme](../Readme.md#tareas-programadas-cron-de-la-vm).
