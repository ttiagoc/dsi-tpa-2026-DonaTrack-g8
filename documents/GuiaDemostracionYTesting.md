# Guía de Demostración y Testing — DonaTrack
**Diseño de Sistemas de Información (UTN FRBA) — TPA 1, TPA 2 y TPA 3**

Este documento detalla **cómo utilizar el conjunto de tests y herramientas de demostración** para defender el proyecto ante los docentes, explicando qué comandos ejecutar, qué verificar en cada paso y cómo justificar técnicamente cada decisión de diseño y persistencia.

---

## 1. Requisitos Previos y Entorno

Antes de comenzar la demostración, asegurate de tener:

1. **Docker Desktop:** Abierto y activo en tu computadora.
2. **Contenedores Levantados:**
   ```bash
   docker compose --profile full up -d
   ```
   *(Podés verificar con `docker compose ps` que los 4 contenedores: `donatrack-postgres`, `donatrack-donaciones`, `donatrack-logistica` y `donatrack-notificaciones` figuren en estado `Up`).*
3. **Java 21 en la terminal:**
   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@21
   export PATH="$JAVA_HOME/bin:$PATH"
   ```

---

## 2. Las 3 Alternativas de Demostración

Tenés tres formas complementarias para mostrar el sistema según el tiempo y formato que prefieran los docentes:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        SUITE DE DEMOSTRACIÓN                           │
├─────────────────────────┬───────────────────────┬──────────────────────┤
│  1. Demo en Terminal    │  2. Pruebas por API   │  3. Tests con Maven  │
│  ./demo_presentacion.sh │  Postman Collection   │  mvn test (JUnit 5)  │
│  (Rápida y visual)      │  (Detallada por HTTP) │  (Formal automatizada)│
└─────────────────────────┴───────────────────────┴──────────────────────┘
```

---

### Opción 1: Demostración en Vivo con el Script (`demo_presentacion.sh`)

Es la opción más recomendada si los docentes piden una **demo funcional rápida**. En 15 segundos recorre todo el ciclo de vida del negocio, llamando a los microservicios y mostrando las tablas de PostgreSQL.

#### Ejecución:
```bash
./demo_presentacion.sh
```

#### Qué ir explicando mientras corre el script:

1. **[Paso 1 - TPA 1] Catálogo y Unidad Mínima:**
   * "Damos de alta la categoría *Alimentos Perecederos* y la subcategoría *Fideos Tallarines*, que actúa como la unidad mínima de asignación del sistema".
2. **[Paso 2 - TPA 1] Donantes Humanos y Jurídicos:**
   * "Creamos a Ana Pérez (Persona Humana) con sus contactos obligatorios y opcionales, y a Arcos Plateados S.A. (Persona Jurídica) con CUIT y representante".
3. **[Paso 3 - TPA 1] Segmentación Automática:**
   * "El donante hace una carga única. El `SegmentadorDeDonacion` la divide automáticamente por subcategoría y fecha de vencimiento, dejando la donación en estado `EN_DEPOSITO`".
4. **[Paso 4 - TPA 1] Entidades y Necesidades:**
   * "Registramos el Comedor Infantil Escobar con una necesidad periódica semanal de fideos".
5. **[Paso 5 - TPA 2] Algoritmo de Matchmaking:**
   * "Ejecutamos el motor de asignación. Combina los algoritmos de *Compatibilidad Semántica* y *Prioridad a Subatendidos*, genera la propuesta y al aceptarla la donación transiciona a `ASIGNACION_REALIZADA`".
6. **[Paso 6 - TPA 2] Logística y Telemetría GPS:**
   * "Damos de alta un camión con chofer, planificamos la ruta con sus paradas ordenadas, iniciamos el recorrido (`EN_TRASLADO`), reportamos coordenadas GPS en tiempo real y confirmamos la entrega (`ENTREGADA`)".
7. **[Paso 7 - TPA 2] Notificaciones:**
   * "Se registra el evento de entrega exitosa en el servicio de notificaciones para avisarle a la donante".
8. **[Paso 8 - TPA 3] Verificación Directa en PostgreSQL:**
   * "Consultamos en vivo las tablas de PostgreSQL en `localhost:5432` demostrando que los datos están persistidos, con el historial de auditoría de estados ordenado".

---

### Opción 2: Demostración de APIs con Postman

Ideal si los profesores quieren ver los **contratos HTTP/REST** y probar endpoints específicos.

1. Abrí **Postman** y hacé click en **Import**.
2. Seleccioná el archivo:
   ```text
   documents/DonaTrack.postman_collection.json
   ```
3. La colección ya viene organizada en 7 carpetas:
   - `01 - Catálogo`: Creación y consulta de categorías.
   - `02 - Donantes`: Alta de donantes humanos y jurídicos.
   - `03 - Donaciones y Segmentación`: Carga de donaciones y consulta de estados.
   - `04 - Entidades Beneficiarias y Necesidades`: Alta de comedores y requerimientos semanales.
   - `05 - Matchmaking`: Ejecución de algoritmos y aprobación de propuestas.
   - `06 - Logística`: Flota, rutas, telemetría GPS y confirmación de paradas.
   - `07 - Notificaciones`: Envío de mensajes y auditoría.

---

### Opción 3: Tests Automatizados con JUnit y Maven (`mvn test`)

Para demostrar la **calidad de software, testing de integración y cobertura de persistencia**:

#### A. Test E2E que escribe en PostgreSQL Real:
Ejecuta todo el flujo llamando a los microservicios y valida con consultas SQL directas vía JDBC que los objetos existen en PostgreSQL:
```bash
mvn test -Dtest=PostgresE2EVerificationTest -pl donaciones-service
```

#### B. Test Integral de Dominio y Persistencia JPA (HSQLDB):
Verifica las reglas de negocio y el ciclo de vida ORM descartando la caché de primer nivel (`nuevoRequest()`):
```bash
mvn test -Dtest=FlujoIntegralTpaTest -pl donaciones-service
```

#### C. Correr toda la suite de pruebas del repositorio:
```bash
mvn test
```

---

## 3. Consultas SQL en Vivo (Base de Datos)

Para proyectar las tablas de la base de datos durante la defensa (en DBeaver, DataGrip, pgAdmin o terminal):

Abrí el archivo:
```text
documents/consultas_auditoria.sql
```

### Consultas más impactantes para mostrar:

1. **Trazabilidad completa de la donación:**
   ```bash
   docker exec -it donatrack-postgres psql -U postgres -d donaciones -c \
   "SELECT d.id, d.estado_actual, s.nombre AS subcategoria, eb.razon_social AS entidad FROM donacion d JOIN subcategoria s ON d.subcategoria_id = s.id LEFT JOIN entidad_beneficiaria eb ON d.entidad_beneficiaria_asignada_id = eb.id ORDER BY d.id DESC LIMIT 5;"
   ```

2. **Auditoría inmutable de cambios de estado (`cambio_estado`):**
   ```bash
   docker exec -it donatrack-postgres psql -U postgres -d donaciones -c \
   "SELECT orden, estado, justificacion, fecha FROM cambio_estado ORDER BY donacion_id DESC, orden ASC LIMIT 8;"
   ```

3. **Flota de camiones y rutas en Logística:**
   ```bash
   docker exec -it donatrack-postgres psql -U postgres -d logistica -c \
   "SELECT id, patente, chofer_nombre || ' ' || chofer_apellido AS chofer, capacidad_carga FROM camion;"
   ```

---

## 4. Preguntas Frecuentes y Justificaciones de Diseño (Machete para la Defensa)

Si los evaluadores hacen preguntas sobre arquitectura y persistencia:

### 1. ¿Por qué no hay Foreign Keys entre el esquema `donaciones` y `logistica`?
> *"Porque seguimos el principio de **Database-per-Service** propio de una arquitectura de microservicios. Cada servicio es dueño exclusivo de su esquema y de sus reglas de negocio. Enlazar esquemas con claves foráneas físicas acoplaría los despliegues a nivel de base de datos. En su lugar, usamos identificadores numéricos (`Long`) como referencias lógicas y la integración se realiza mediante contratos HTTP/REST."*

### 2. ¿Qué estrategia de herencia usaron para `Donante` (`PersonaHumana` y `PersonaJuridica`)?
> *"Implementamos la estrategia `SINGLE_TABLE` (`@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`). Esta alternativa evita JOINs complejos al consultar donantes polimórficos y optimiza notablemente la performance de lectura, utilizando una columna discriminadora `tipo_donante`."*

### 3. ¿Cómo modelaron el historial de cambios de estado de la donación?
> *"Utilizamos un `@ElementCollection` con `@OrderColumn(name = 'orden')` mapeado a la tabla `cambio_estado`. Cada cambio de estado es un **Value Object inmutable** que registra fecha, estado, justificación y camión. De esta forma, el historial completo se persiste de manera secuencial y auditable sin sobreescribir estados previos."*

### 4. ¿Cómo diferencian las necesidades recurrentes de las extraordinarias?
> *"Aplicamos el **patrón Strategy** con la interfaz `TipoNecesidad`. `NecesidadRecurrente` valida la satisfacción dentro del período temporal correspondiente (por ejemplo, semanal o mensual), mientras que `NecesidadExtraordinaria` acumula donaciones parciales hasta cubrir o superar la cantidad requerida."*

### 5. ¿Cómo garantizan que los tests no ensucien la base de producción local?
> *"El reactor multi-módulo utiliza dos configuraciones independientes: en los tests automatizados (`mvn test`) Hibernate corre sobre **HSQLDB en memoria** con rollback y truncado entre tests; mientras que en el entorno de desarrollo y despliegue corre sobre **PostgreSQL 16** alojado en Docker Compose con persistencia en volúmenes."*
