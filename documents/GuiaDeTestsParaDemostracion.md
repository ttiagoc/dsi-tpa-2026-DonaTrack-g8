# Guía de Uso de Tests para la Demostración — DonaTrack
**Diseño de Sistemas de Información (UTN FRBA) — TPA 1, TPA 2 y TPA 3**

Esta guía está diseñada para que cualquier miembro del equipo pueda realizar una **defensa en vivo impecable**, demostrando el funcionamiento real del sistema mediante los tests automatizados, scripts de ejecución y consultas a la base de datos PostgreSQL.

---

## 1. Los 3 Instrumentos de Demostración

Para la entrega disponemos de 3 formas de probar y mostrar el sistema:

| Instrumento | Ubicación | Tipo | Cuándo Usarlo |
|---|---|---|---|
| **Test E2E en PostgreSQL** | [PostgresE2EVerificationTest.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/test/java/ar/edu/utn/frba/ddsi/donaciones/PostgresE2EVerificationTest.java) | JUnit 5 + HTTP + JDBC | **Ideal para la defensa técnica:** llama a los microservicios reales y valida directamente en las tablas de PostgreSQL. |
| **Test Integral JPA (Dominio)** | [FlujoIntegralTpaTest.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/test/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/jpa/FlujoIntegralTpaTest.java) | JUnit 5 + JPA + HSQLDB | **Ideal para preguntas de diseño/ORM:** valida patrones, entidades, repositorios y persistencia sin levantar Docker. |
| **Script Interactivo de Consola** | [demo_presentacion.sh](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/demo_presentacion.sh) | Bash + cURL + psql | **Ideal para demostración visual rápida (15 seg):** simula todo el ciclo de vida del negocio con colores y tablas en terminal. |

---

## 2. Preparación Previa a la Presentación (30 segundos)

Antes de que te llamen los profesores, abrí la terminal en la raíz del proyecto y ejecutá:

```bash
# 1. Configurar Java 21 en la terminal
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Verificar que Docker tenga los 4 contenedores arriba
docker compose --profile full up -d
docker compose ps
```

> **Verificación Visual:** Los cuatro contenedores deben figurar en estado `Up`:
> - `donatrack-postgres` (Puerto 5432)
> - `donatrack-donaciones` (Puerto 8080)
> - `donatrack-logistica` (Puerto 8081)
> - `donatrack-notificaciones` (Puerto 8082)

---

## 3. Demostración Paso a Paso usando los Tests

### Estrategia Recomendada: Disposición de Pantalla

> **Tip de Presentación:** Dividí la pantalla en **dos mitades**:
> - **Lado Izquierdo:** Terminal para correr los tests de Maven o el script.
> - **Lado Derecho:** DBeaver / DataGrip o una segunda terminal con `psql` para ver los registros insertados en tiempo real.

---

### DEMO A: Test E2E contra PostgreSQL Real (`PostgresE2EVerificationTest`)

Este test es la prueba más contundente de que el proyecto funciona como un sistema distribuido real.

#### 1. Comando de ejecución:
```bash
mvn test -Dtest=PostgresE2EVerificationTest -pl donaciones-service
```

#### 2. Qué hace el test internamente:
1. Realiza llamadas HTTP `POST` a los microservicios en los puertos 8080, 8081 y 8082.
2. Abre una conexión JDBC nativa contra `jdbc:postgresql://localhost:5432/donaciones` y `logistica`.
3. Ejecuta queries `SELECT` para certificar que las filas se persistieron en las tablas relacionales.

#### 3. Libreto / Guion para relatar a los docentes mientras corre:
* **Al iniciar:** *"Profesores, este test ejecuta de punta a punta todo el circuito del TPA 1, TPA 2 y TPA 3 comunicándose con los 3 microservicios levantados en Docker y verificando directamente en PostgreSQL."*
* **Al ver `[Paso 1] Creando Catalogo`:** *"Cumpliendo con TPA 1, damos de alta la categoría 'Alimentos' y su subcategoría, que definimos como la unidad mínima indivisible de asignación."*
* **Al ver `[Paso 2] Registrando Donantes`:** *"Creamos un donante Persona Humana con sus medios de contacto, y una Persona Jurídica con CUIT y representante comercial."*
* **Al ver `[Paso 3] Segmentacion automatica`:** *"El donante carga una lista heterogénea de bienes. El `SegmentadorDeDonacion` divide la carga por subcategoría y fecha de caducidad, dejando cada donación en estado `EN_DEPOSITO`."*
* **Al ver `[Paso 4 y 5] Entidades y Matchmaking`:** *"El motor de matchmaking cruza la necesidad del Comedor con la donación disponible y genera la propuesta sugerida. Al aprobarla, el estado transiciona a `ASIGNACION_REALIZADA`."*
* **Al ver `[Paso 6 y 7] Logistica y Notificaciones`:** *"En el servicio de Logística planificamos la ruta para un camión, registramos telemetría GPS, confirmamos la entrega física pasando la parada a `ENTREGADA`, y se dispara la notificación al donante."*
* **Al ver `[Paso 8] Verificacion Directa en PostgreSQL (JDBC)`:** *"Finalmente, el test se conecta a PostgreSQL por JDBC y comprueba que las tablas `donante`, `donacion`, `camion` y `ruta` tienen registros consistentes. El resultado es `BUILD SUCCESS`."*

#### 4. Validación inmediata en PostgreSQL (Mostrar en vivo):
Apenas termina el test, podés ejecutar este comando para mostrar los registros recién creados:
```bash
docker exec -it donatrack-postgres psql -U postgres -d donaciones -c \
"SELECT d.id, d.estado_actual, s.nombre AS subcategoria, eb.razon_social AS comedor FROM donacion d JOIN subcategoria s ON d.subcategoria_id = s.id LEFT JOIN entidad_beneficiaria eb ON d.entidad_beneficiaria_asignada_id = eb.id ORDER BY d.id DESC LIMIT 3;"
```

---

### DEMO B: Test Integral de Dominio y Persistencia JPA (`FlujoIntegralTpaTest`)

Usalo si los docentes piden ver el **diseño orientado a objetos**, los **patrones de diseño**, las **anotaciones JPA** o cómo manejan las transacciones.

#### 1. Comando de ejecución:
```bash
mvn test -Dtest=FlujoIntegralTpaTest -pl donaciones-service
```

#### 2. Puntos clave para señalar en el código ([FlujoIntegralTpaTest.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/test/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/jpa/FlujoIntegralTpaTest.java)):

1. **Estrategia de Persistencia Limpia (`nuevoRequest()`):**
   * Señalar las llamadas a `nuevoRequest()`.
   * *Explicación:* *"Para evitar falsos positivos por la caché de primer nivel de Hibernate, cerramos y reabrimos el `EntityManager`. Si el test pasa, garantiza que los datos fueron efectivamente serializados a la base de datos relacional y recuperados mediante SQL."*

2. **Segmentación y Reglas de Negocio (Línea 114):**
   * Señalar el uso de `SegmentadorDeDonacion`.
   * *Explicación:* *"El donante ingresa una carga única con fideos, puré de tomate y sillas. El segmentador instancia 3 objetos `Donacion` independientes, cada una con su propio ciclo de vida e historial de estados."*

3. **Patrón Strategy para Necesidades (Línea 138 y 149):**
   * Mostrar `NecesidadRecurrente(Periodo.SEMANAL)` vs `NecesidadExtraordinaria()`.
   * *Explicación:* *"Modelamos el comportamiento de satisfacción de necesidades con el patrón Strategy, desacoplando la lógica de recurrencia periódica de las necesidades extraordinarias por contingencias."*

4. **Auditoría Inmutable de Estados con `@ElementCollection` (Línea 212):**
   * Mostrar `donacionFinal.getHistorialEstados()`.
   * *Explicación:* *"El historial no sobrescribe el estado anterior; cada cambio es un Value Object inmutable (`CambioEstado`) persistido en una tabla secundaria con `@OrderColumn(name = 'orden')`."*

---

### DEMO C: Demostración Interactiva con Script (`demo_presentacion.sh`)

Si los docentes quieren ver una **demostración visual en vivo** sin entrar al código fuente:

```bash
./demo_presentacion.sh
```

El script imprimirá paso por paso con colores, formateando JSONs de respuesta y finalizando con una consulta SQL directa que muestra:
1. Donaciones activas y asignadas.
2. Auditoría cronológica de cambios de estado (`cambio_estado`).
3. Flota de camiones y choferes en el microservicio de logística.

---

## 4. Cuadro Comparativo: Consignas de la Cátedra vs Qué Test lo Valida

Si los docentes preguntan puntualmente por una consigna, usá este mapa:

| Consigna Cátedra | Concepto Requerido | Implementado en | Validado por |
|---|---|---|---|
| **TPA 1 - Req 1** | Catálogo jerárquico, unidad mínima | [Categoria.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donaciones/Categoria.java), [Subcategoria.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donaciones/Subcategoria.java) | `FlujoIntegralTpaTest.java` (Línea 60)<br>`PostgresE2EVerificationTest.java` (Línea 48) |
| **TPA 1 - Req 2** | Donantes Humano y Jurídico, Contactos obligatorios | [PersonaHumana.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/PersonaHumana.java), [PersonaJuridica.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/PersonaJuridica.java) | `FlujoIntegralTpaTest.java` (Línea 70)<br>`PostgresE2EVerificationTest.java` (Línea 60) |
| **TPA 1 - Req 3** | Carga única y Segmentación automática | [SegmentadorDeDonacion.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donaciones/SegmentadorDeDonacion.java) | `FlujoIntegralTpaTest.java` (Línea 105)<br>`PostgresE2EVerificationTest.java` (Línea 80) |
| **TPA 1 - Req 4** | Estados de donación y trazabilidad inmutable | [Donacion.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donaciones/Donacion.java), [CambioEstado.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donaciones/CambioEstado.java) | `FlujoIntegralTpaTest.java` (Línea 186)<br>`demo_presentacion.sh` (Paso 8) |
| **TPA 1 - Req 5** | Necesidades Recurrentes y Extraordinarias | [Necesidad.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/entidades/Necesidad.java), [TipoNecesidad.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/entidades/TipoNecesidad.java) | `FlujoIntegralTpaTest.java` (Línea 129)<br>`PostgresE2EVerificationTest.java` (Línea 98) |
| **TPA 2 - Algoritmos** | Matchmaking (Semántica + Subatendidos) | [GeneradorPropuestas.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donaciones/GeneradorPropuestas.java) | `FlujoIntegralTpaTest.java` (Línea 163)<br>`PostgresE2EVerificationTest.java` (Línea 116) |
| **TPA 2 - Logística** | Rutas, Paradas ordenadas, Telemetría GPS | [Ruta.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/Ruta.java), [Parada.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/Parada.java) | `PostgresE2EVerificationTest.java` (Línea 150)<br>`demo_presentacion.sh` (Paso 6) |
| **TPA 2 - Avisos** | Notificaciones multicanal (Email, SMS, WhatsApp) | [NotificacionController.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/notificaciones-service/src/main/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificacionController.java) | `PostgresE2EVerificationTest.java` (Línea 197)<br>`demo_presentacion.sh` (Paso 7) |
| **TPA 3 - Persistencia** | Mapeo ORM JPA, Repositorios, PostgreSQL | [JpaDonacion.java](file:///Users/francoball/Estudio/UTN/3año/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/impl/JpaDonacion.java) | `FlujoIntegralTpaTest.java` (Todo el archivo)<br>`PostgresE2EVerificationTest.java` (Todo el archivo) |

---

## 5. Respuestas Rápidas para Preguntas Frecuentes de los Docentes

### Q1: *"¿Por qué tienen dos tests con bases de datos distintas?"*
> **Respuesta:** *"Seguimos las mejores prácticas de la pirámide de testing:
> 1. `FlujoIntegralTpaTest` corre en memoria (HSQLDB) de forma aislada y ultra-rápida en el pipeline de build para validar el mapeo ORM y la lógica de negocio pura sin dependencias de infraestructura.
> 2. `PostgresE2EVerificationTest` es una prueba de integración del sistema distribuido real desplegado en Docker, validando la interacción HTTP entre microservicios y la persistencia en PostgreSQL 16 con el principio Database-per-Service."*

### Q2: *"¿Cómo aseguraron que los donantes no dupliquen datos en la base?"*
> **Respuesta:** *"Mapeamos `Donante` con la estrategia `InheritanceType.SINGLE_TABLE`. Tanto `PersonaHumana` como `PersonaJuridica` comparten la tabla `donante` con una columna discriminadora `tipo_donante`. Los atributos comunes (dirección, medios de contacto predeterminados) no se duplican, y evitamos JOINs costosos en consultas polimórficas."*

### Q3: *"¿Por qué la tabla `cambio_estado` no tiene clave primaria artificial propia (`id`)?"*
> **Respuesta:** *"Porque un `CambioEstado` es un **Value Object** inmutable, no una entidad con identidad independiente. Está mapeado con `@ElementCollection` a través de la clave foránea `donacion_id` y ordenado mediante `@OrderColumn(name = 'orden')`."*

### Q4: *"¿Cómo se comunican Donaciones y Logística si no hay Foreign Keys entre sus bases de datos?"*
> **Respuesta:** *"Para respetar la autonomía de microservicios (Database-per-Service), no existen Foreign Keys físicas entre esquemas distintos. La correlación se realiza mediante referencias lógicas (`idDonacion`, `idEntidad`) transferidas en los contratos JSON vía HTTP/REST."*

---

## 6. Comandos de Rescate Rápido (Cheat Sheet)

```bash
# Correr TODO el build y la suite completa de tests:
mvn clean test

# Correr solo el test E2E contra PostgreSQL:
mvn test -Dtest=PostgresE2EVerificationTest -pl donaciones-service

# Correr solo el test JPA de Dominio:
mvn test -Dtest=FlujoIntegralTpaTest -pl donaciones-service

# Correr la demo en vivo por terminal:
./demo_presentacion.sh

# Ver logs en vivo de un microservicio:
docker logs -f donatrack-donaciones
docker logs -f donatrack-logistica
docker logs -f donatrack-notificaciones
```
