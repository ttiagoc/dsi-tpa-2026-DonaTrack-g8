# Implementación de la Persistencia JPA en `logistica-service` (Entrega 3)

Este documento detalla en profundidad la arquitectura, decisiones de diseño objeto-relacional, configuración y pruebas implementadas en el microservicio **`logistica-service`** para la **Entrega 3: Persistencia**, cumpliendo con la consigna de la cátedra, el [DER.puml](DER.puml), las [JustificacionesDisenoRelacional.md](JustificacionesDisenoRelacional.md) y la [GuiaPersistenciaJPA.md](GuiaPersistenciaJPA.md).

---

## 1. Contexto y Objetivos

La Entrega 3 tiene como objetivo dotar a cada microservicio de persistencia en una base de datos relacional mediante un ORM (JPA con Hibernate y `jpa-extras`), manteniendo esquemas totalmente aislados por servicio:
- Cada servicio posee su propia base de datos (`donaciones`, `notificaciones`, `logistica`).
- No existen Foreign Keys (FKs) físicas entre esquemas de diferentes servicios; las referencias a entidades externas son identificadores simples (`Long`).
- Se utiliza **PostgreSQL** para el despliegue local y **HSQLDB** (en memoria) para la suite de tests unitarios y de integración.
- Para el acceso a datos se utiliza el paquete `jpa-extras` (1.0.0-rc1) de la cátedra, que se basa en **Hibernate 5.6** y el paquete estándar **`javax.persistence.*`**.

---

## 2. Comparativa Arquitectónica: `donaciones-service` vs. `logistica-service`

Tomando `donaciones-service` como referencia previa del proyecto:

| Aspecto | `donaciones-service` | `logistica-service` |
|---|---|---|
| **Base de datos propia** | `donaciones` (puerto 5432) | `logistica` (puerto 5432) |
| **Herencia de Entidades** | `SINGLE_TABLE` en `Donante` (`PersonaHumana`, `PersonaJuridica`) con discriminador `tipo_donante`. | **Sin herencia**: modelo plano de entidades. |
| **Value Objects (`@Embeddable`)** | `MedioContacto`, `Representante`, `Bien`, `CambioEstado`. | `Chofer` y `Ubicacion`. |
| **Colección de datos externos** | Colecciones de entidades locales (`donacion_bienes`, `donacion_fotos_recepcion`, `cambio_estado`). El historial de estados usa `@OrderColumn`, con PK `(donacion_id, orden)`. | `Parada` almacena `donacionIds` (`parada_donaciones`), que son referencias numéricas a donaciones de otro servicio sin integridad referencial forzada por la BD. |
| **Repositorios Base** | `RepositorioJpa<T>` que implementa `WithSimplePersistenceUnit`. | Misma clase base `RepositorioJpa<T>`, abstrayendo transacciones (`withTransaction`) y operaciones CRUD seguras (`save`, `findById`, `findAll`, `deleteById`). |
| **Desacoplamiento JPA** | Repositorios aislados por tabla/entidad. | `JpaCamion` ya no requiere inyectar `RutaRepository` para calcular `findAllDisponibles()`, sino que lo resuelve directamente mediante subconsultas JPQL correlacionadas. |
| **Manejo del ciclo de vida** | `PersistenceContextController` limpia el `EntityManager` ligado al hilo por cada request en Javalin. | Mismo patrón con `PersistenceContextController`. |

---

## 3. Mapeo Objeto-Relacional (ORM)

De acuerdo al diseño del DER físico, el mapeo se realizó respetando las convenciones de la cátedra y las tablas de `logistica`:

### 3.1. Value Objects Embebidos (`@Embeddable`)

1. **`Chofer`** ([Chofer.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/Chofer.java)):
   - Representa al conductor asignado a un camión. No posee ciclo de vida independiente ni endpoints propios.
   - Marcado con `@Embeddable`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Data`.
   - Atributos: `nombre`, `apellido`.

2. **`Ubicacion`** ([Ubicacion.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/Ubicacion.java)):
   - Representa la última coordenada y telemetría registrada del camión. Solo se persiste la última posición conocida (no un historial).
   - Marcado con `@Embeddable`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Data`.
   - Atributos: `latitud`, `longitud`, `timestamp`, `velocidad`.

---

### 3.2. Entidades Persistidas (`@Entity`)

1. **`Camion`** ([Camion.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/Camion.java)):
   - Tabla: `camion`.
   - Clave primaria: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`.
   - Columnas: `patente` (VARCHAR 20), `capacidad_volumen` (DOUBLE), `altura` (DOUBLE), `capacidad_carga` (DOUBLE).
   - Mapeo embebido de `Chofer` mediante `@AttributeOverrides`:
     - `nombre` $\rightarrow$ `chofer_nombre`
     - `apellido` $\rightarrow$ `chofer_apellido`
   - Mapeo embebido de `Ubicacion` mediante `@AttributeOverrides`:
     - `latitud` $\rightarrow$ `ubicacion_latitud`
     - `longitud` $\rightarrow$ `ubicacion_longitud`
     - `timestamp` $\rightarrow$ `ubicacion_timestamp`
     - `velocidad` $\rightarrow$ `ubicacion_velocidad`

2. **`Parada`** ([Parada.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/Parada.java)):
   - Tabla: `parada`.
   - **Justificación de diseño (Entidad vs. Embeddable)**: Aunque funcionalmente una parada es parte de una ruta, se mapea como `@Entity` independiente con PK propia por dos razones fundamentales:
     1. La API direcciona la confirmación de entrega puntualmente por el id de la parada (`POST /api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones`).
     2. Cada parada almacena su propia lista de IDs de donaciones (`List<Long> donacionIds`). En la especificación JPA no es portable anidar una `@ElementCollection` dentro de un `@Embeddable` que a su vez esté en otra `@ElementCollection`.
   - Columna `entidad_id`: hace referencia a `entidad_beneficiaria` en `donaciones-service` como número simple, **sin FK física**.
   - Colección `donacionIds`:
     ```java
     @ElementCollection
     @CollectionTable(name = "parada_donaciones", joinColumns = @JoinColumn(name = "parada_id"))
     @Column(name = "donacion_id")
     private List<Long> donacionIds = new ArrayList<>();
     ```

3. **`Ruta`** ([Ruta.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/Ruta.java)):
   - Tabla: `ruta`.
   - Clave primaria: `id` (BIGINT IDENTITY).
   - Columnas: `fecha` (DATE), `estado` (`@Enumerated(EnumType.STRING)` VARCHAR 20).
   - Relación con Camión:
     - `@ManyToOne @JoinColumn(name = "camion_id") private Camion camion;`
     - *Nota*: La ruta no almacena chofer propio, sino que lo consulta a través de su camión asociado.
   - Relación con Paradas:
     - La ruta es la dueña del ciclo de vida de sus paradas:
     ```java
     @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
     @JoinColumn(name = "ruta_id")
     @OrderBy("orden ASC")
     private List<Parada> paradas = new ArrayList<>();
     ```
     - Se utiliza `@OrderBy("orden ASC")` para garantizar el orden de entrega al recuperar la lista de paradas desde la base de datos.

---

## 4. Configuración y Dependencias

### 4.1. Dependencias Maven ([pom.xml](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/pom.xml))
1. **Fijación de `jaxb-runtime` a 2.3.1**:
   Spring Boot 4 eleva `jaxb-runtime` a 4.x (namespace Jakarta), lo cual resulta incompatible con Hibernate 5.6 (`javax`). Se forzó la versión en `<dependencyManagement>`:
   ```xml
   <dependencyManagement>
       <dependencies>
           <dependency>
               <groupId>org.glassfish.jaxb</groupId>
               <artifactId>jaxb-runtime</artifactId>
               <version>2.3.1</version>
           </dependency>
       </dependencies>
   </dependencyManagement>
   ```
2. **Dependencias del ORM y Bases de Datos**:
   - `io.github.flbulgarelli:jpa-extras:1.0.0-rc1`
   - `org.postgresql:postgresql` (scope `runtime`)
   - `org.hsqldb:hsqldb` (scope `test`)

### 4.2. Unidad de Persistencia ([persistence.xml](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/resources/META-INF/persistence.xml))
- Nombre de la unidad: `simple-persistence-unit` (requerido por `WithSimplePersistenceUnit`).
- Proveedor: `org.hibernate.jpa.HibernatePersistenceProvider`.
- Clases registradas: `Chofer`, `Ubicacion`, `Camion`, `Ruta` y `Parada`.
- Propiedades: `hibernate.hbm2ddl.auto = update`, `format_sql = true`, `show_sql = false`.

### 4.3. Configuración de Conexión y Ciclo de Vida
- **[application.properties](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/resources/application.properties)**:
  Variables parametrizadas para PostgreSQL local con valores por defecto:
  ```properties
  logistica.db.driver=org.postgresql.Driver
  logistica.db.url=${LOGISTICA_DB_URL:jdbc:postgresql://localhost:5432/logistica}
  logistica.db.username=${LOGISTICA_DB_USERNAME:postgres}
  logistica.db.password=${LOGISTICA_DB_PASSWORD:postgres}
  logistica.db.dialect=org.hibernate.dialect.PostgreSQL10Dialect
  logistica.db.hbm2ddl=update
  ```
- **[PersistenceConfig.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/config/PersistenceConfig.java)**:
  Inicializa `WithSimplePersistenceUnit.configure(...)` dinámicamente según el entorno (`Environment` de Spring).
- **[PersistenceContextController.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/controllers/PersistenceContextController.java)**:
  `jpa-extras` mantiene un `EntityManager` por hilo. Como el servidor Javalin reutiliza hilos de trabajo para atender requests entrantes, en `app.after(...)` se ejecuta `WithSimplePersistenceUnit.dispose()` para evitar fugas de memoria o que un request lea datos cacheados de un request anterior. Si ocurrió una excepción en el pipeline, realiza `rollbackTransaction()` antes del dispose.

---

## 5. Implementación de los Repositorios JPA

Se sustituyeron las implementaciones en memoria (`InMemoryCamion`, `InMemoryRuta`) por implementaciones reales con JPA:

### 5.1. Repositorio Base Genérico ([RepositorioJpa.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/repositories/impl/RepositorioJpa.java))
Provee la lógica transaccional estándar para cualquier entidad `T`:
- `save(T entidad)`: si el ID es nulo, invoca `persist(entidad)`. Si ya está administrada en la sesión actual, la devuelve directamente. Si está desasociada (detached), ejecuta `merge(entidad)`. Todo dentro de `withTransaction(...)`.
- `findById(Long id)`: consulta por ID con `find(clase, id)`.
- `findAll()`: `from <Clase>`.
- `deleteById(Long id)`: busca la entidad y la remueve dentro de una transacción.
- `existsById(Long id)`.

### 5.2. `JpaCamion` ([JpaCamion.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/repositories/impl/JpaCamion.java))
Implementa `CamionRepository`:
- **`findByPatente(String patente)`**:
  ```java
  createQuery("from Camion where lower(patente) = lower(:patente)", Camion.class)
      .setParameter("patente", patente.trim())
      .getResultStream()
      .findFirst();
  ```
- **`findAllDisponibles()`**:
  Resuelve la disponibilidad sin necesidad de dependencias en memoria con `RutaRepository`. Un camión está disponible si no existe ninguna ruta asociada a él que no esté finalizada:
  ```java
  createQuery("select c from Camion c where not exists ("
          + "select r from Ruta r where r.camion = c and r.estado <> :finalizada)", Camion.class)
      .setParameter("finalizada", EstadoRuta.FINALIZADA)
      .getResultList();
  ```

### 5.3. `JpaRuta` ([JpaRuta.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/repositories/impl/JpaRuta.java))
Implementa `RutaRepository`:
- **`buscarRutasActivas()`**:
  `from Ruta where estado = :enTraslado` con `:enTraslado = EstadoRuta.EN_TRASLADO`.
- **`buscarRutasActivasPorCamion(Long idCamion)`**:
  `from Ruta where camion.id = :id and estado <> :finalizada`.
- **`buscarRutaDelCamion(Long idCamion)`**:
  `from Ruta where camion.id = :id`, devolviendo la primera ruta o lanzando `IllegalStateException("El camión no tiene ninguna ruta.")` si no se encuentra.

---

## 6. Estrategia de Testing Automatizado

Para garantizar la correctitud del mapeo y las consultas JPQL sin requerir un servidor PostgreSQL levantado en cada ejecución de CI/CD:

1. **Configuración de Tests** ([application-test.properties](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/test/resources/application-test.properties)):
   - Base de datos en memoria HSQLDB: `jdbc:hsqldb:mem:logistica-test`.
   - Dialecto: `org.hibernate.dialect.HSQLDialect`.
   - `hbm2ddl=create-drop`.
2. **Clase Base de Persistencia** ([PersistenciaTest.java](file:///d:/Usuario/Desktop/Estudio/UTN/3%20A%C3%B1o/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/logistica-service/src/test/java/ar/edu/utn/frba/ddsi/logistica/models/repositories/jpa/PersistenciaTest.java)):
   - Configura la unidad de persistencia con HSQLDB.
   - En `@BeforeEach`, limpia la base mediante `TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK` para asegurar aislamiento e idempotencia entre tests.
   - Provee el método `nuevoRequest()` (`WithSimplePersistenceUnit.dispose()`), el cual descarta la caché de primer nivel del `EntityManager`, forzando a que la siguiente lectura consulte obligatoriamente a la base de datos.
3. **Tests de Repositorios**:
   - **`CamionRepositoryJpaTest`**: verifica guardado y lectura de camión con chofer y ubicación, búsqueda case-insensitive por patente, filtrado de camiones disponibles según estado de rutas y borrado por ID.
   - **`RutaRepositoryJpaTest`**: verifica guardado de ruta con camión y paradas en cascada, verificación de orden ascendente en paradas, guardado de donaciones entregadas, consultas de rutas activas y eliminación en cascada de paradas al borrar la ruta.
4. **Validación de Tests Previos**:
   - Se corroboró que todos los tests unitarios de lógica de negocio previos (`CamionTest`, `GestorDeRutasTest`, `MonitorDeRutasTest`, `PlanificadorDeRutasTest`, `RutaTest` y `LogisticaServiceApplicationTests`) continúen pasando sin modificaciones.

---

## 7. Resultados de Ejecución

Al ejecutar `mvn test` desde el directorio raíz del proyecto:

```text
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for ddsi-tp-template 1.0-SNAPSHOT:
[INFO] 
[INFO] ddsi-tp-template ................................... SUCCESS [  0.003 s]
[INFO] common-lib ......................................... SUCCESS [  1.504 s]
[INFO] donaciones-service ................................. SUCCESS [ 10.552 s]
[INFO] logistica-service .................................. SUCCESS [  8.486 s]
[INFO] notificaciones-service ............................. SUCCESS [  7.084 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  28.247 s
```

En `logistica-service`:
- **Tests ejecutados**: 24
- **Fallos**: 0
- **Errores**: 0
- **Skipped**: 0
