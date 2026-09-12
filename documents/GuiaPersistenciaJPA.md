# Guía: persistencia con JPA + jpa-extras (Entrega 3)

Esta guía es para quienes implementan la persistencia de **notificaciones-service** y **logistica-service**. `donaciones-service` ya está hecho y sirve de referencia: cuando algo no quede claro, mirá cómo está resuelto ahí.

Documentos relacionados:
- [DiagramaER.puml](DiagramaER.puml): las tablas de cada servicio.
- [JustificacionesDisenoRelacional.md](JustificacionesDisenoRelacional.md): por qué cada tabla quedó así.

---

## 1. Cambios que ya están hechos y te afectan

Antes de arrancar, traé los últimos cambios de `main` y corré `mvn clean install` desde la raíz.

| Dónde | Qué cambió | Por qué |
|---|---|---|
| `pom.xml` (padre) | Se importa `jetty-bom` 11.0.20 antes del BOM de Spring Boot. | Javalin 6 necesita Jetty 11 y Spring Boot 4 forzaba Jetty 12. **Ningún servicio arrancaba** (`NoSuchMethodError` al iniciar Javalin). No lo saques. |
| `common-lib` | `MedioContacto` ahora es `@Embeddable` y common-lib depende de `javax.persistence-api`. | Para poder embeberlo en las tablas de cada servicio. |
| logística: `Ruta` | Se eliminó el campo `chofer`. | Duplicaba `Camion.chofer` y nunca se completaba. La ruta consulta el chofer a su camión. |
| logística: `Parada` | Tiene `id : Long`. `ParadaResponse` lo devuelve. | `POST /api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones` ahora busca por **id**, no por `orden`. |

---

## 2. Cómo se integra jpa-extras (paso a paso)

### Punto importante: se usa `javax.persistence`, NO `jakarta.persistence`

`jpa-extras` (1.0.0-rc1, la del template de la cátedra) trae Hibernate 5.6, que usa `javax.persistence.*`. El IDE suele sugerir `jakarta.persistence.*` porque Spring Boot 4 usa Jakarta: **no lo aceptes**, porque las anotaciones no se van a reconocer.

### Paso 1: dependencias en el `pom.xml` del servicio

```xml
<!-- jpa-extras trae Hibernate 5.6 (javax). El BOM de Spring Boot 4 sube jaxb-runtime a 4.x
     (jakarta), incompatible con Hibernate 5.6: se fija la version que Hibernate espera. -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.glassfish.jaxb</groupId>
            <artifactId>jaxb-runtime</artifactId>
            <version>2.3.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- ...las que ya tiene el servicio... -->

    <dependency>
        <groupId>io.github.flbulgarelli</groupId>
        <artifactId>jpa-extras</artifactId>
        <version>1.0.0-rc1</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.hsqldb</groupId>
        <artifactId>hsqldb</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Sin el bloque de `jaxb-runtime`, Hibernate falla al iniciar.

### Paso 2: `src/main/resources/META-INF/persistence.xml`

Listá **todas** las clases mapeadas, incluidas las `@Embeddable` y `MedioContacto` si la usás. La conexión no va acá: la carga `PersistenceConfig` (paso 4).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="http://java.sun.com/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://java.sun.com/xml/ns/persistence
    http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
             version="2.0">

  <persistence-unit name="simple-persistence-unit" transaction-type="RESOURCE_LOCAL">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>

    <class>ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto</class>
    <class>ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion</class>
    <exclude-unlisted-classes>true</exclude-unlisted-classes>

    <properties>
      <property name="hibernate.show_sql" value="false"/>
      <property name="hibernate.format_sql" value="true"/>
      <property name="hibernate.hbm2ddl.auto" value="update"/>
    </properties>
  </persistence-unit>

</persistence>
```

El nombre `simple-persistence-unit` es obligatorio: es el que busca `WithSimplePersistenceUnit`.

### Paso 3: anotar las entidades

Mismo estilo que en clase: `@Entity`, `@Table`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Column(name = "...")`, `@Embedded`, `@ElementCollection`, `@ManyToOne`, `@OneToMany`, `@Enumerated(EnumType.STRING)`. En la [sección 3](#3-mapeo-de-cada-servicio-según-el-der) está el detalle de cada tabla.

Toda entidad y todo embeddable necesita **constructor sin argumentos** (`@NoArgsConstructor`).

### Paso 4: configuración de la conexión

`config/PersistenceConfig.java` (cambiá el prefijo `donaciones` por el de tu servicio):

```java
@Configuration
public class PersistenceConfig {

    public PersistenceConfig(Environment env) {
        try {
            WithSimplePersistenceUnit.configure(properties -> properties
                    .set("hibernate.connection.driver_class", env.getRequiredProperty("logistica.db.driver"))
                    .set("hibernate.connection.url", env.getRequiredProperty("logistica.db.url"))
                    .set("hibernate.connection.username", env.getRequiredProperty("logistica.db.username"))
                    .set("hibernate.connection.password", env.getProperty("logistica.db.password", ""))
                    .set("hibernate.dialect", env.getRequiredProperty("logistica.db.dialect"))
                    .set("hibernate.hbm2ddl.auto", env.getProperty("logistica.db.hbm2ddl", "update"))
                    .set("hibernate.show_sql", env.getProperty("logistica.db.show-sql", "false")));
        } catch (IllegalStateException e) {
            // La unidad de persistencia ya fue inicializada (por ejemplo, por otro test en la misma JVM)
        }
    }
}
```

`src/main/resources/application.properties` (cada servicio tiene **su propia base**):

```properties
logistica.db.driver=org.postgresql.Driver
logistica.db.url=${LOGISTICA_DB_URL:jdbc:postgresql://localhost:5432/logistica}
logistica.db.username=${LOGISTICA_DB_USERNAME:postgres}
logistica.db.password=${LOGISTICA_DB_PASSWORD:postgres}
logistica.db.dialect=org.hibernate.dialect.PostgreSQL10Dialect
logistica.db.hbm2ddl=update
logistica.db.show-sql=false
```

Para notificaciones: base `notificaciones` y variables `NOTIFICACIONES_DB_*`.

### Paso 5: liberar el EntityManager al terminar cada request

`jpa-extras` guarda un EntityManager **por hilo**. Javalin reutiliza hilos, así que si no se libera, un request puede ver entidades viejas cacheadas de otro. Copiá `controllers/PersistenceContextController.java`:

```java
@Component
public class PersistenceContextController implements JavalinController, WithSimplePersistenceUnit {

    @Override
    public void registerRoutes(Javalin app) {
        app.after(ctx -> {
            try {
                WithSimplePersistenceUnit.dispose();
            } catch (IllegalStateException transaccionAbierta) {
                // un error dejo una transaccion sin terminar: se descarta antes de liberar
                rollbackTransaction();
                WithSimplePersistenceUnit.dispose();
            }
        });
    }
}
```

### Paso 6: repositorios JPA

1. Copiá `RepositorioJpa.java` de `donaciones-service/.../models/repositories/impl/` a tu servicio. Resuelve `save`, `findById`, `findAll`, `deleteById` y `existsById`.
2. Por cada interfaz de repositorio, creá una implementación `Jpa<Nombre>` que extienda `RepositorioJpa` y agregue las consultas propias:

```java
@Repository
public class JpaCamion extends RepositorioJpa<Camion> implements CamionRepository {

    public JpaCamion() {
        super(Camion.class);
    }

    @Override
    public Optional<Camion> findByPatente(String patente) {
        return createQuery("from Camion where patente = :patente", Camion.class)
                .setParameter("patente", patente)
                .getResultStream()
                .findFirst();
    }
}
```

3. **Borrá la clase `InMemory<Nombre>`** que reemplaza. Si quedan dos `@Repository` para la misma interfaz, Spring no arranca.

### Paso 7: tests con HSQLDB

`src/test/resources/application-test.properties`:

```properties
logistica.db.driver=org.hsqldb.jdbcDriver
logistica.db.url=jdbc:hsqldb:mem:logistica-test
logistica.db.username=sa
logistica.db.password=
logistica.db.dialect=org.hibernate.dialect.HSQLDialect
logistica.db.hbm2ddl=create-drop
```

Para los tests de repositorios, copiá `PersistenciaTest.java` de `donaciones-service/src/test/.../models/repositories/jpa/` (cambiando la URL de HSQLDB). Esa clase configura HSQLDB, vacía la base antes de cada test y ofrece `nuevoRequest()`, que descarta la caché para forzar la lectura desde la base. Ejemplo de uso en `DonanteRepositoryJpaTest`:

```java
class CamionRepositoryJpaTest extends PersistenciaTest {
    private final JpaCamion repositorio = new JpaCamion();

    @Test
    void guardaYLeeCamionConChofer() {
        Long id = repositorio.save(new Camion("AB123CD", 20.0, 3.5, 8000.0, new Chofer("Juan", "Perez"))).getId();
        nuevoRequest();

        assertEquals("Juan", repositorio.findById(id).orElseThrow().getChofer().getNombre());
    }
}
```

Los tests con `@SpringBootTest` tienen que llevar `@ActiveProfiles("test")`, así toman HSQLDB. `JavalinWebServer` tiene `@Profile("!test")`, por lo que en los tests Javalin **no** arranca solo. Si querés probar la API por HTTP, mirá `DonacionesApiPersistenciaTest`.

---

## 3. Mapeo de cada servicio según el DER

### notificaciones-service (base `notificaciones`)

**`notificacion`**: `@Entity`
- `fechaDeEnvio` → `@Column(name = "fecha_de_envio")`
- `mensaje` → `@Column(length = 500)`
- `contacto` (`MedioContacto`) → `@Embedded` con overrides:
  ```java
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "valor", column = @Column(name = "contacto_valor")),
      @AttributeOverride(name = "tipoContacto", column = @Column(name = "contacto_tipo_contacto", length = 20))
  })
  private MedioContacto contacto;
  ```
- `completada` → columna normal

### logistica-service (base `logistica`)

**`camion`**: `@Entity`
- `chofer` (`Chofer` → `@Embeddable`) con overrides `chofer_nombre`, `chofer_apellido`.
- `ubicacion` (`Ubicacion` → `@Embeddable`) con overrides `ubicacion_latitud`, `ubicacion_longitud`, `ubicacion_timestamp`, `ubicacion_velocidad`. Solo se guarda la última posición.
- `capacidadVolumen`, `capacidadCarga` → `@Column(name = "capacidad_volumen")`, etc.

**`ruta`**: `@Entity`
- `estado` → `@Enumerated(EnumType.STRING)`
- `camion` → `@ManyToOne @JoinColumn(name = "camion_id")`
- `paradas`: la ruta es dueña de sus paradas.
  ```java
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "ruta_id")
  @OrderBy("orden ASC")
  private List<Parada> paradas;
  ```

**`parada`**: `@Entity`. No puede ser `@Embeddable` porque tiene su propia lista de ids.
- `entidadId` → `@Column(name = "entidad_id")`. Es un id de donaciones-service: **sin FK ni `@ManyToOne`**, porque es otro servicio y otra base.
- `donacionIds` → tabla `parada_donaciones`:
  ```java
  @ElementCollection
  @CollectionTable(name = "parada_donaciones", joinColumns = @JoinColumn(name = "parada_id"))
  @Column(name = "donacion_id")
  private List<Long> donacionIds;
  ```

**Consultas de los repositorios de logística** (hoy se resuelven filtrando en memoria):

| Método | JPQL sugerido |
|---|---|
| `CamionRepository.findAllDisponibles()` | `select c from Camion c where not exists (select r from Ruta r where r.camion = c and r.estado <> :finalizada)` |
| `RutaRepository.buscarRutasActivas()` | `from Ruta where estado = :enTraslado` |
| `RutaRepository.buscarRutasActivasPorCamion(id)` | `from Ruta where camion.id = :id and estado <> :finalizada` |
| `RutaRepository.buscarRutaDelCamion(id)` | `from Ruta where camion.id = :id`: tomar el primero o lanzar `IllegalStateException`, como hoy. |

Con JPA, `JpaCamion` ya no necesita recibir el `RutaRepository` como `InMemoryCamion`: la consulta resuelve la disponibilidad sola. `InMemoryRuta` asignaba ids a las paradas a mano; con JPA eso lo hace la base.

---

## 4. Errores que ya nos pasaron (para que no pierdas tiempo)

1. **`jakarta.persistence` en vez de `javax.persistence`**: las anotaciones se ignoran y aparecen errores del tipo "no es una entidad".
2. **Falta el bloque de `jaxb-runtime`**: Hibernate explota al iniciar.
3. **Clase no listada en `persistence.xml`**: "Unknown entity" o el embeddable no se reconoce. Acordate de `MedioContacto`.
4. **`merge` sobre una entidad que ya está administrada** con listas inmutables (`List.of(...)`): `UnsupportedOperationException`. `RepositorioJpa.save` ya lo evita. No lo cambies.
5. **Lombok `@Data` con relaciones bidireccionales**: `toString`/`hashCode` se llaman en bucle hasta un `StackOverflowError`. Poné `@ToString.Exclude` y `@EqualsAndHashCode.Exclude` en uno de los dos lados (ver `RegistroDonacion.donante`).
6. **Orden de las listas**: la base no garantiza el orden. Si importa (paradas por `orden`, historial por fecha), usá `@OrderBy`.
7. **Colecciones lazy en otros hilos**: dentro del request funcionan porque el EntityManager sigue abierto. No pases entidades a un `CompletableFuture` para que las recorra después: armá el DTO o request antes, en el hilo del request (como hacen los listeners de donaciones).
8. **FKs entre servicios**: no existen. Cualquier referencia a datos de otro servicio es un `Long` simple.
9. **Solo técnicas vistas en clase**: nada de `CHECK` ni mapeos en XML (`orm.xml`). Todo con anotaciones.

---

## 5. Checklist antes de hacer el PR

- [ ] `mvn clean install` desde la raíz pasa con todos los tests en verde.
- [ ] Todas las entidades y embeddables están listados en `persistence.xml`.
- [ ] Imports `javax.persistence.*` (ningún `jakarta.persistence`).
- [ ] Se borraron las clases `InMemory*` reemplazadas.
- [ ] `PersistenceConfig`, `PersistenceContextController` y `application-test.properties` agregados.
- [ ] Hay al menos un test de repositorio por entidad contra HSQLDB.
- [ ] Las tablas y columnas coinciden con [DiagramaER.puml](DiagramaER.puml). Si tuviste que cambiar algo, actualizá el DER y su párrafo en [JustificacionesDisenoRelacional.md](JustificacionesDisenoRelacional.md).
- [ ] Probado contra PostgreSQL local con su propia base (`logistica` / `notificaciones`).
