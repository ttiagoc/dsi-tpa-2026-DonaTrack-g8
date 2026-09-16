# ddsi-tp-template

Plantilla base para el trabajo práctico de DDSI (UTN FRBA). Implementa una arquitectura de servicios con Spring Boot y una biblioteca compartida, usando un reactor de Maven multi-módulo.

---

## Requisitos previos

- JDK 21
- Maven 3.9+
- Docker (para levantar PostgreSQL en el despliegue local)

### Credenciales de Twilio y Resend

`notificaciones-service` necesita `src/main/resources/application.yaml` con las claves de
Twilio y Resend. Ese archivo está gitignoreado (no se sube), así que en un clone nuevo **no
existe**, y sin él el servicio no arranca: `Could not resolve placeholder 'resend.apiKey'`.

Hay que crearlo copiando la plantilla:

```bash
cp notificaciones-service/src/main/resources/application-example.yaml \
   notificaciones-service/src/main/resources/application.yaml
```

Con las claves de ejemplo el servicio levanta y los tests pasan, pero no puede enviar mails ni
SMS de verdad: para eso hay que reemplazarlas por las credenciales reales.

---

## Estructura del repositorio

```
dsi-tpa-2026-DonaTrack-g8/
├── pom.xml                    # POM padre: versiones y dependencyManagement
├── docker-compose.yml         # PostgreSQL local (+ los servicios, con el perfil "full")
├── docker/postgres/           # Script de inicialización de las bases
├── common-lib/                # Librería compartida (JAR), importada por los servicios
├── donaciones-service/        # Servicio de donaciones — puerto 8080
├── logistica-service/         # Servicio de logística — puerto 8081
└── notificaciones-service/    # Servicio de notificaciones — puerto 8082
```

Cada servicio es una aplicación Spring Boot independiente que declara `common-lib` como dependencia local del reactor.

---

## Tecnologías

| Tecnología          | Versión       |
|---------------------|---------------|
| Java                | 21            |
| Spring Boot         | 4.0.5         |
| Spring Cloud BOM    | 2025.1.1      |
| Lombok              | 1.18.34       |
| Maven               | 3.9+          |

El BOM de Spring Cloud está declarado en el POM padre para que los módulos puedan incorporar dependencias de Spring Cloud sin especificar versión explícita.

---

## Desarrollo local (Maven)

Todos los comandos se ejecutan desde la **raíz del proyecto**.

### Compilar todos los módulos

```bash
mvn clean install
```

Esto construye `common-lib` primero y luego los servicios que dependen de ella.

Para el día a día conviene evitar el `clean` y compilar los módulos en paralelo, que es
bastante más rápido (los tres servicios son independientes entre sí, así que `-T 1C` los
compila a la vez; `-o` evita ir a la red a revisar dependencias ya descargadas):

```bash
mvn -T 1C install -DskipTests    # compilar
mvn -T 1C -o test                # correr la suite
```

En este proyecto eso baja la suite completa de unos 30s a unos 14s.

### Ejecutar un servicio

Los servicios necesitan PostgreSQL corriendo (ver la sección siguiente).

```bash
# Servicio de donaciones (puerto 8080)
mvn spring-boot:run -pl donaciones-service

# Servicio de logística (puerto 8081)
mvn spring-boot:run -pl logistica-service

# Servicio de notificaciones (puerto 8082)
mvn spring-boot:run -pl notificaciones-service
```

Maven resuelve `common-lib` directamente desde el reactor, por lo que no hace falta instalarla por separado si se ejecuta desde la raíz.

---

## Base de datos

Cada servicio tiene **su propia base**, sin entidades ni FKs compartidas entre ellas: las
referencias entre esquemas (por ejemplo `parada.entidad_id` en logística) son solo el valor
del id, sin integridad referencial a nivel motor.

| Servicio               | Base             | Testing                  |
|------------------------|------------------|--------------------------|
| donaciones-service     | `donaciones`     | HSQLDB en memoria        |
| logistica-service      | `logistica`      | HSQLDB en memoria        |
| notificaciones-service | `notificaciones` | HSQLDB en memoria        |

El mapeo se hace con JPA (Hibernate) sobre el paquete `jpa-extras`, y la unidad de
persistencia de cada servicio está declarada en su `META-INF/persistence.xml`. La conexión
no se fija ahí: la arma `PersistenceConfig` a partir de `application.properties`.

Los tests de persistencia no levantan Spring, así que configuran HSQLDB por su cuenta en la
clase base `PersistenciaTest` de cada servicio. No hace falta ninguna base corriendo para
`mvn test`.

### Levantar PostgreSQL local

Los pasos detallados, con verificaciones y problemas frecuentes, están en
[documents/LevantarBaseDeDatos.md](documents/LevantarBaseDeDatos.md). El resumen:

```bash
docker compose up -d
```

Esto arranca PostgreSQL 16 en `localhost:5432` (usuario y contraseña `postgres`) y, la
primera vez, crea las tres bases con `docker/postgres/init-databases.sql`. Los datos quedan
en el volumen `donatrack_postgres-data`.

Las tablas **no** hay que crearlas a mano: cada servicio corre con `hbm2ddl=update`, así que
Hibernate genera su esquema. Ojo que lo hace de forma *lazy*, en el primer request que toca la
base: hasta entonces las tablas no existen, aunque los servicios estén arriba.

Comandos útiles:

```bash
docker compose ps                  # estado y healthcheck
docker compose logs -f postgres    # logs del motor
docker compose down                # detener (los datos se conservan)
docker compose down -v             # detener y BORRAR los datos (re-ejecuta el init)
```

Para abrir una consola SQL contra una de las bases:

```bash
docker exec -it donatrack-postgres psql -U postgres -d donaciones
```

### Apuntar a otro PostgreSQL

Los valores por defecto de `application.properties` se pueden sobreescribir con variables de
entorno, sin tocar código:

```bash
DONACIONES_DB_URL=jdbc:postgresql://mi-host:5432/donaciones
DONACIONES_DB_USERNAME=usuario
DONACIONES_DB_PASSWORD=secreto
```

Lo mismo con los prefijos `LOGISTICA_DB_*` y `NOTIFICACIONES_DB_*`.

### Todo en contenedores

Los tres servicios están declarados en `docker-compose.yml` bajo el perfil `full`, de modo
que `docker compose up -d` por sí solo levanta únicamente la base:

```bash
docker compose --profile full up -d --build
```

En ese modo los servicios se hablan por el nombre del servicio de compose
(`http://donaciones-service:8080/api`) en lugar de `localhost`, porque dentro de cada
contenedor `localhost` es el contenedor mismo.

---

## Construcción de imágenes Docker

Este proyecto utiliza una arquitectura multi-módulo de Maven. Los microservicios dependen del `pom.xml` padre y de `common-lib`, por lo que **el contexto de construcción de Docker siempre debe ser la raíz del proyecto**. Si se limita el contexto a la carpeta del microservicio, Maven fallará al no encontrar el POM padre ni las dependencias comunes.

### Construcción manual (CLI)

Posicionarse en la carpeta raíz del proyecto y pasar el Dockerfile con `-f`, dejando `.` como contexto:

```bash
# donaciones-service (expone el puerto 8080)
docker build -t donaciones-img -f donaciones-service/Dockerfile .

# notificaciones-service (expone el puerto 8081)
docker build -t notificaciones-img -f notificaciones-service/Dockerfile .
```

### Ejecutar los contenedores

```bash
docker run -p 8080:8080 donaciones-img
docker run -p 8081:8081 notificaciones-img
```

### Nota sobre `ARG SERVICE_NAME`

Cada Dockerfile define un `ARG SERVICE_NAME` cuyo valor por defecto ya coincide con el nombre del servicio (p. ej. `donaciones-service`). Solo es necesario sobreescribirlo si se reutiliza un Dockerfile genérico para construir un servicio diferente:

```bash
docker build --build-arg SERVICE_NAME=otro-service -f otro-service/Dockerfile .
```

---

## Estado del proyecto

Los servicios son aplicaciones Spring Boot mínimas, listas para extender con controladores, repositorios y lógica de negocio. `common-lib` contiene el código compartido entre servicios.
