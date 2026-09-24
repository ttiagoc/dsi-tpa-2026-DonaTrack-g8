# Cómo levantar la base de datos

Guía paso a paso para dejar el proyecto corriendo contra PostgreSQL en tu máquina.

Cada servicio tiene **su propia base**, sin entidades ni claves foráneas compartidas entre
ellas (requerimiento 1.1 de la Entrega 3):

| Servicio               | Puerto | Base de datos    |
|------------------------|--------|------------------|
| donaciones-service     | 8080   | `donaciones`     |
| logistica-service      | 8081   | `logistica`      |
| notificaciones-service | 8082   | `notificaciones` |

Las tres viven en la misma instancia de PostgreSQL, que se levanta con Docker.

> Para correr los **tests** no hace falta nada de esto: usan HSQLDB en memoria.
> `mvn test` funciona con la base apagada.

---

## Requisitos

- **JDK 21**
- **Maven 3.9+**
- **Docker Desktop** — tiene que estar **corriendo**, no solo instalado

Verificá que estén los tres:

```bash
java -version      # debe decir 21
mvn -version
docker info        # si falla, abrí Docker Desktop y esperá que termine de arrancar
```

---

## Paso 1 — Credenciales de Twilio y Resend

Solo la primera vez, y **solo si nunca lo hiciste**.

`notificaciones-service` necesita un `application.yaml` con las claves de Twilio y Resend. Ese
archivo **no está en el repo** (está gitignoreado, porque son credenciales), así que en un
clone nuevo no existe. Sin él el servicio no arranca y vas a ver:

```
Could not resolve placeholder 'resend.apiKey'
```

Creálo copiando la plantilla:

```bash
cp notificaciones-service/src/main/resources/application-example.yaml \
   notificaciones-service/src/main/resources/application.yaml
```

Con las claves de ejemplo el servicio levanta bien y los tests pasan. Solo no va a poder
enviar mails ni SMS de verdad; para eso hay que pedirle las credenciales reales al grupo y
reemplazarlas.

---

## Paso 2 — Levantar PostgreSQL

Desde la **raíz del proyecto**:

```bash
docker compose up -d
```

La primera vez se descarga la imagen de PostgreSQL 16 (unos 100 MB), así que tarda un rato.

Esto arranca el motor en `localhost:5432` con usuario y contraseña `postgres`, y crea las tres
bases ejecutando `docker/postgres/init-databases.sql`.

**Esperá a que esté `healthy`** antes de seguir:

```bash
docker compose ps
```

```
NAME                 STATUS
donatrack-postgres   Up 12 seconds (healthy)
```

Si dice `health: starting`, esperá unos segundos y repetí. Arrancar los servicios antes de que
la base esté lista hace que fallen al conectarse.

Comprobá que estén las tres bases:

```bash
docker exec donatrack-postgres psql -U postgres -c "\l"
```

Tienen que aparecer `donaciones`, `logistica` y `notificaciones`.

---

## Paso 3 — Compilar

```bash
mvn clean install -DskipTests
```

Compila `common-lib` primero y después los tres servicios.

---

## Paso 4 — Arrancar los servicios

Cada uno en **su propia terminal**, porque quedan corriendo en primer plano:

```bash
mvn spring-boot:run -pl donaciones-service       # terminal 1, puerto 8080
mvn spring-boot:run -pl logistica-service        # terminal 2, puerto 8081
mvn spring-boot:run -pl notificaciones-service   # terminal 3, puerto 8082
```

Cada uno está listo cuando imprime algo así:

```
Javalin web server started on port 8080
Started DonacionesServiceApplication in 3.3 seconds
```

---

## Paso 5 — Verificar las tablas

Las tablas **se crean solas al arrancar cada servicio**, no hay que escribir ningún DDL a mano
ni mandar ningún request.

`jpa-extras` crea el `EntityManagerFactory` de forma *lazy* (recién en el primer uso), así que
`PersistenceConfig` lo fuerza durante el arranque. En ese momento Hibernate genera el esquema:
cada servicio corre con `hbm2ddl=update`, que crea lo que falta y no borra datos existentes.

Como consecuencia, **si la base no está disponible el servicio no arranca** (falla al
conectarse en vez de fallar en el primer request).

Verificá que estén las tablas:

```bash
docker exec donatrack-postgres psql -U postgres -d donaciones -c "\dt"
docker exec donatrack-postgres psql -U postgres -d logistica -c "\dt"
docker exec donatrack-postgres psql -U postgres -d notificaciones -c "\dt"
```

Tienen que dar **16**, **4** y **1** tablas respectivamente, 21 en total, que son exactamente
las del diagrama entidad-relación (`DER.puml`).

---

## Consultar la base a mano

Para abrir una consola SQL:

```bash
docker exec -it donatrack-postgres psql -U postgres -d donaciones
```

Comandos útiles de `psql`:

| Comando        | Qué hace                          |
|----------------|-----------------------------------|
| `\dt`          | lista las tablas                  |
| `\d donante`   | muestra la estructura de una tabla|
| `\l`           | lista las bases                   |
| `\q`           | salir                             |

---

## Apagar todo

Cortá los servicios con `Ctrl+C` en cada terminal, y después:

```bash
docker compose down        # apaga la base, CONSERVA los datos
docker compose down -v     # apaga la base y BORRA los datos
```

Los datos viven en un volumen de Docker llamado `donatrack_postgres-data`, no en la carpeta del
proyecto. Sobreviven a `docker compose down` y a reiniciar la máquina.

Usá `down -v` cuando quieras empezar de cero: borra el volumen y en el próximo `up` se vuelve a
ejecutar el script que crea las tres bases.

---

## Problemas frecuentes

**`docker compose` falla con "failed to connect to the docker API"**
Docker Desktop no está corriendo. Abrilo y esperá a que el ícono deje de estar en movimiento.

**Los servicios no se conectan a la base**
Casi siempre es que arrancaron antes de que PostgreSQL terminara de inicializar. Verificá con
`docker compose ps` que diga `(healthy)` y reiniciá el servicio. Como el esquema se genera al
arrancar, en este caso el servicio directamente no levanta.

**Las tablas no existen**
Revisá que el servicio haya terminado de arrancar sin errores: las tablas se crean durante el
arranque, así que si no están es que el servicio no llegó a conectarse a su base.

**`Could not resolve placeholder 'resend.apiKey'` al arrancar notificaciones**
Falta el Paso 1: el `application.yaml` de notificaciones.

**`port is already allocated` o `Address already in use`**
Ya hay algo en el 5432 (otro PostgreSQL instalado en la máquina) o en el 8080/8081/8082 (un
servicio que quedó colgado de una corrida anterior). En Windows, para ver quién ocupa un puerto:

```powershell
netstat -ano | findstr :5432
```

**Las bases `logistica` y `notificaciones` no se crearon**
El script `init-databases.sql` corre **solo** cuando el volumen está vacío, o sea la primera
vez. Si el volumen ya existía de antes, corré `docker compose down -v` y después `up -d`.

---

## Apuntar a otro PostgreSQL

Si en vez del contenedor querés usar un PostgreSQL ya instalado, no hace falta tocar código:
los valores por defecto se sobreescriben con variables de entorno.

```bash
DONACIONES_DB_URL=jdbc:postgresql://mi-host:5432/donaciones
DONACIONES_DB_USERNAME=usuario
DONACIONES_DB_PASSWORD=secreto
```

Lo mismo con los prefijos `LOGISTICA_DB_*` y `NOTIFICACIONES_DB_*`. En ese caso las tres bases
hay que crearlas a mano (las tablas las sigue creando Hibernate).
