# Justificaciones y Consideraciones de Diseño Relacional — DonaTrack

Este documento acompaña a [DiagramaER.puml](DiagramaER.puml) y explica las decisiones de mapeo objeto-relacional y de diseño de datos tomadas para la Entrega 3 (Persistencia), incluyendo los casos donde se optó deliberadamente por desnormalizar el modelo.

## Metodología

Cada decisión de mapeo (herencia, embeddable vs. entidad, desnormalización) se evalúa respondiendo cuatro preguntas concretas, en vez de aplicar una regla general de memoria. Los primeros tres puntos surgen de una consulta a la cátedra (Fernando Petryszyn, ayudante) sobre cómo persistir `PersonaHumana`/`PersonaJuridica`; el cuarto surge de esa misma conversación, al preguntar puntualmente si desnormalizar significa resignar reglas de negocio (como un `NOT NULL`) a nivel de base de datos:

1. **¿Qué queries se van a hacer realmente sobre esta clase?** — se releen los controllers/servicios para ver los accesos reales, no los que "podrían" existir.
2. **¿El volumen de esas queries justifica optimizar para ellas?** — en un sistema con esta escala, lo que importa no es tanto el volumen sino evitar JOINs/queries innecesarios en el camino más transitado de la API.
3. **¿Esa clase está en una parte crítica que podría ser un cuello de botella, o es un camino secundario?** — si es secundario, se prioriza el modelo más normalizado/simple aunque no sea el más performante.
4. **¿Cuál es el costo concreto de desnormalizar, y vale la pena pagarlo?** — desnormalizar no es gratis: normalmente implica resignar validaciones que el motor de base de datos podría imponer solo (ej. un `NOT NULL` real por subtipo) y mover ese costo a mantenimiento futuro. Es aceptable **siempre que quede documentado el motivo** y, en la medida de lo posible, se compense con constraints a nivel de esquema (`CHECK`) en vez de confiar únicamente en la validación de la capa de aplicación.

## Convenciones generales

- **Estrategia de generación de PK**: `BIGINT` con `GenerationType.IDENTITY` en todas las tablas, por simplicidad y porque es soportada de forma nativa tanto por HSQLDB (testing) como por PostgreSQL/MariaDB (despliegue local).
- **Separación por microservicio**: cada servicio (`donaciones-service`, `logistica-service`, `notificaciones-service`) tiene su propio esquema de datos. No existen entidades ni tablas compartidas entre esquemas; la comunicación entre servicios se resuelve por API/eventos, nunca por acceso directo a la base de otro servicio.
- **Value objects como `@Embeddable`**: `MedioContacto` y `Ubicacion` no tienen identidad propia ni ciclo de vida independiente del objeto que las contiene (no hay ningún caso de uso que las busque, liste o edite por sí solas), por lo que se mapean como `@Embeddable` en vez de entidades con tabla propia:
  - Campo único → `@Embedded` (columnas embebidas directo en la tabla dueña).
  - Campo de tipo lista (ej. `Donante.contactos`, `EntidadBeneficiaria.correoRepresentantes`) → `@ElementCollection` (tabla auxiliar con FK al dueño, sin PK propia por fila).
  - Nota: si en el futuro se necesitara trazabilidad histórica de `Ubicacion` (recorrido completo de un camión, no solo la última posición), correspondería reconvertirla a entidad con FK a `Camion`.

## Decisión: herencia de `Donante` (`PersonaHumana` / `PersonaJuridica`) → `SINGLE_TABLE`

### Contexto

`Donante` es una clase abstracta con dos subtipos concretos, `PersonaHumana` (`nombre`, `apellido`, `fechaNacimiento`, `dni`, `genero`, `direccion`) y `PersonaJuridica` (`razonSocial`, `rubro`, `tipo`, `cuit`, `representantes`). La API (`DonanteController`) expone:

- `GET /api/donantes` — lista **todos** los donantes, mezclando ambos tipos.
- `GET /api/donantes/{id}` — obtiene un donante por id sin conocer de antemano su tipo concreto.
- Alta y modificación sí están separadas por tipo (`POST /api/donantes/persona-humana`, `POST /api/donantes/persona-juridica`, etc.).

Es decir: **la lectura es predominantemente polimórfica**, mientras que la escritura siempre pasa por el subtipo concreto.

### Alternativas evaluadas

| Estrategia | Ventajas | Desventajas |
|---|---|---|
| `SINGLE_TABLE` (una tabla `donante` + columna discriminadora `tipo_donante`) | Lecturas polimórficas (`obtenerTodos`, `obtenerPorId`) sin ningún JOIN — el caso de uso más frecuente. Simplicidad de mapeo e índices. | Columnas específicas de un subtipo quedan `NULL` para el otro. La restricción "estas columnas son obligatorias para este tipo" no la impone el schema por sí solo. |
| `JOINED` (tabla `donante` común + `persona_humana`/`persona_juridica` con FK 1:1) | Normalizado: ninguna columna de más, se pueden declarar `NOT NULL` reales por subtipo. Escala mejor si aparece un tercer subtipo con muchos atributos propios. | Cada lectura de un donante (el caso más común) requiere un JOIN adicional. Mayor complejidad de mapeo para un beneficio que hoy no se aprovecha. |
| `TABLE_PER_CLASS` (una tabla por subtipo, sin tabla común) | Ninguna columna `NULL`, cada tabla autosuficiente. | `obtenerTodos()` requiere `UNION ALL` entre tablas — la peor opción justo para la query más usada. Generación de IDs entre tablas más incómoda. Desaconsejada en general por la bibliografía salvo caso puntual. Descartada. |

### Decisión y justificación

Se eligió **`SINGLE_TABLE`** porque el patrón de acceso real está dominado por lecturas polimórficas, y esa es la única estrategia que las resuelve sin JOIN ni UNION. Es una **desnormalización deliberada**: se acepta redundancia de columnas `NULL` a cambio de simplicidad y performance de lectura en el caso de uso dominante.

### Riesgo de inconsistencia y mitigación

La objeción principal a `SINGLE_TABLE` es que, al vivir todas las columnas en una sola tabla, nada impide —a nivel de motor de base de datos— que una fila `PERSONA_HUMANA` tenga cargado un `cuit`, o le falte el `dni`. En este proyecto ese riesgo está acotado porque:

1. Cada esquema es privado de su microservicio: el único camino de escritura es a través de las entidades JPA.
2. Las clases `PersonaHumana` y `PersonaJuridica` **no exponen los atributos del otro subtipo** (ej. `PersonaHumana` no tiene el campo `cuit`), por lo que Hibernate nunca genera un `INSERT`/`UPDATE` que toque esas columnas para el subtipo que no corresponde — la inconsistencia no puede darse por uso normal de la aplicación.

### Costo de desnormalizar (punto 4 de la Metodología)

Se consultó puntualmente a la cátedra si desnormalizar significa resignar reglas de negocio a nivel de base de datos (ej.: ¿es incorrecto no tener `dni` obligatorio para una persona humana?). La respuesta: no es incorrecto en sí mismo, pero **el costo tiene que estar documentado**. En este caso el costo que se asume es concreto: `SINGLE_TABLE` no permite declarar a nivel de esquema que `dni`/`nombre`/`apellido` son obligatorios solo para `PERSONA_HUMANA` (ni que `cuit`/`razon_social` lo son para `PERSONA_JURIDICA`) — esa validación queda a cargo exclusivamente de la capa de aplicación (los DTOs `PersonaHumanaRequest`/`PersonaJuridicaRequest` y el servicio), no del motor de base de datos. Se acepta ese costo porque, como se explicó arriba, el único camino de escritura es la aplicación misma.

## Decisión: `Representante` → `@Embeddable`

`Representante` (lista dentro de `PersonaJuridica`) no tiene `id` propio ni en la clase de dominio ni en su DTO (`RepresentanteRequest`), y no existe ningún endpoint que lo busque, liste o edite de forma aislada — siempre se reemplaza como parte completa de `PersonaJuridicaRequest.representantes()`. Mismo criterio que `MedioContacto`/`Ubicacion`: no tiene sentido de existencia propio, fuera del donante al que pertenece.

Se mapea como `@Embeddable` vía `@ElementCollection` (tabla `donante_representantes`, FK a `donante.id`, sin PK propia por fila), aplicando solo a filas donde `tipo_donante = 'PERSONA_JURIDICA'`. Un detalle particular: `Representante.correo` es a su vez un `MedioContacto` — un embeddable anidado dentro de otro embeddable —, lo cual JPA soporta sin problema; se aplana igual en las columnas (`correo_valor`, `correo_tipo_contacto`).

## Decisión: `RegistroDonacion` → no se persiste (objeto transitorio)

Checklist aplicado (queries reales / volumen / cuello de botella — ver sección Metodología):

1. **Queries reales**: ninguna. No existe repositorio para `RegistroDonacion` (a diferencia de `Donacion`, `Donante`, `EntidadBeneficiaria`). El único punto donde se instancia es `DonacionServiceImpl.crear()`, que lo arma a partir del request, se lo pasa a `SegmentadorDeDonacion.segmentarDonacion(...)` para partirlo en una o más `Donacion` (agrupando `Bien` por `generarKey()`), y lo descarta — solo el resultado (`List<Donacion>`) se persiste.
2. **Volumen**: no aplica, no hay filas que guardar.
3. **Cuello de botella**: no, vive y muere dentro de un único método.

Consistente con que la clase de dominio no tiene `id`. No se mapea con JPA (ni `@Entity` ni `@Embeddable`): es un objeto de comando/agrupador interno, cuyo único rol es servir de entrada a `SegmentadorDeDonacion`.

> **Nota aparte** (no es una decisión de persistencia, es un hallazgo de código): ver punto 1 de [ErroresACorregir.md](ErroresACorregir.md) — `Donante.donaciones`/`agregarDonacion()` no se usan en ningún flujo real.

## Decisión: `Bien` → `@Embeddable` + `@ElementCollection`

Checklist aplicado: **queries reales** — ninguna aislada; ni `BienRequest` ni `BienResponse` tienen `id`, y no hay endpoint en `DonacionController` que opere sobre un `Bien` puntual, siempre viaja como parte completa de `Donacion.bienes`. **Volumen** — no justifica pagar el JOIN de una tabla propia para algo que siempre se lee en bloque junto a su `Donacion`. **Cuello de botella** — no, mismo motivo.

Se mapea igual que `donante_contactos`: tabla `donacion_bienes`, FK a `donacion.id`, sin PK propia por fila. `Bien.subcategoria` es un `@ManyToOne` real al catálogo de `Subcategoria` (ver decisión siguiente).

## Decisión: `Categoria` / `Subcategoria` → catálogo real (`@Entity`), no `@Embeddable`

### Primera lectura (revisada)

En un primer pase se había mapeado como `@Embeddable` anidado: ni `Categoria` ni `Subcategoria` tenían `id` en el código, no existía `CategoriaController`/`SubcategoriaController` ni repositorio, y `DonacionServiceImpl.toSubcategoria()` construía una instancia **nueva** a partir del request cada vez, sin buscar ninguna existente por nombre. Por el checklist estricto ("¿qué usa el código hoy?"), esa lectura era correcta.

### Por qué se corrigió a catálogo real

Al revisar el diagrama de clases original, las cuatro asociaciones (`Subcategoria --> Categoria`, `Bien --> Subcategoria`, `Donacion --> Subcategoria`, `Necesidad --> Subcategoria`) apuntan todas a la misma `Subcategoria` — la intención del modelo de dominio era una referencia compartida, no una copia por cada uso. El costo que había quedado anotado en la primera versión (dos `Bien`/`Donacion`/`Necesidad` con el mismo `subcategoria.nombre` podían terminar con `pideEstado`/`esPerecedero` inconsistentes, al no haber una única fuente de verdad) se consideró no aceptable para esta entrega, y se decidió resolverlo con un catálogo real — lo que **excede el mapeo JPA y requiere cambios de código**, asumidos deliberadamente porque el objetivo de esta entrega es la persistencia correcta del modelo:

- `Categoria.java` / `Subcategoria.java`: se les agregó `id : Long`.
- Nuevos `CategoriaRepository`/`SubcategoriaRepository` (+ implementación `InMemory*`, mismo patrón que el resto del proyecto) con `findByNombre(String)`.
- `DonacionServiceImpl.toCategoria()`/`toSubcategoria()` y `EntidadBeneficiariaServiceImpl.toCategoria()`/`toSubcategoria()` pasaron de "crear siempre nueva" a **buscar por nombre; si no existe, crearla** (`findByNombre(...).orElseGet(() -> repo.save(new ...))`) — semántica de upsert-por-nombre, no un endpoint de ABM separado.

### Modelo resultante

Tablas `categoria` (`id`, `nombre` único, `pide_estado`, `es_perecedero`) y `subcategoria` (`id`, `nombre` único, `categoria_id` FK). `Bien`, `Donacion` y `Necesidad` pasan a tener `subcategoria_id` (`@ManyToOne` real) en vez de columnas embebidas duplicadas.

**Nota para el entregable 1**: esto también implica actualizar el diagrama de clases para reflejar que `Categoria`/`Subcategoria` tienen `id` propio y son referenciadas (no copiadas) por `Bien`, `Donacion` y `Necesidad`.

## Decisión: `Donacion` → `@Entity`

A diferencia de `RegistroDonacion` y `Bien`, `Donacion` sí es una entidad real: tiene `id`, repositorio propio (`DonacionRepository`) y endpoints propios completos en `DonacionController` (`GET /api/donaciones`, `GET /api/donaciones/{id}`, `POST /api/donaciones`, `DELETE`, cambio de estado, etc.). `entidad_beneficiaria_asignada_id` nace `NULL` y se completa recién cuando el matchmaking asigna una entidad beneficiaria (ver `Donacion.obtenerDireccion()`, que devuelve `null` mientras el estado sea `EN_DEPOSITO`).

## Decisión: `CambioEstado` (historial de `Donacion`) → `@Embeddable` + `@ElementCollection`

Checklist aplicado: **queries reales** — ninguna sobre un `CambioEstado` aislado; `EstadoDonacionResponse` solo usa el **último** elemento de la lista (`donacion.estadoActual()` + la fecha del último `CambioEstado`), no existe ningún endpoint que devuelva el historial completo. **Volumen** — la lista crece con cada `cambiarEstado()` (es un log de auditoría real, no descartable como `RegistroDonacion`), pero sigue sin haber ningún consumidor que la consulte fuera del contexto de su `Donacion`. **Cuello de botella** — no.

Se mapea igual que `Bien`: tabla `donacion_historial_estados`, FK a `donacion.id`, sin PK propia por fila — es una tabla append-only (nunca se actualiza ni borra una fila existente, solo se agregan).

## Decisión: `Donacion.fotosRecepcion` → `@ElementCollection` de tipo básico

Es una `List<String>` (URLs de fotos), sin identidad individual y sin ningún caso de uso que referencie una foto puntual — se agregan en bloque vía `subirFotosRecepcion()`. Al ser un tipo básico (`String`), no hace falta `@Embeddable`: `@ElementCollection` alcanza directo, tabla `donacion_fotos_recepcion` con FK a `donacion.id`.

## Decisión: `EntidadBeneficiaria` → `@Entity`

Entidad real: tiene `id`, repositorio propio y CRUD completo en `EntidadBeneficiariaController`. `telefono` se mapea `@Embedded` (mismo criterio que `Donante.contactoPredeterminado`) y `correoRepresentantes` como `@ElementCollection` (mismo patrón que `donante_contactos`).

## Decisión: `Necesidad` → `@Entity` (a diferencia de `Bien`)

Checklist aplicado: **queries reales** — a diferencia de `Bien`, acá sí hay endpoints dedicados que direccionan por el `id` propio de la `Necesidad`: `GET/POST /api/entidad-beneficiaria/{entidadId}/necesidades` y, sobre todo, `DELETE .../necesidades/{necesidadId}` — que borra una `Necesidad` puntual por su id. Eso solo tiene sentido si tiene identidad y tabla propia.

`Necesidad.subcategoria` es `@ManyToOne` al catálogo real de `Subcategoria`, igual que en `Bien`/`Donacion` (ver decisión correspondiente — `EntidadBeneficiariaServiceImpl.toSubcategoria()` reutiliza exactamente la misma lógica de búsqueda-o-creación que `DonacionServiceImpl`).

### `TipoNecesidad` (`NecesidadExtraordinaria` / `NecesidadRecurrente`) — aplanado manual, no herencia JPA

`Necesidad.tipoNecesidad` no es que `Necesidad` herede de algo — es un campo de tipo interfaz (`TipoNecesidad`), implementado por dos clases (`NecesidadExtraordinaria`, sin estado propio; `NecesidadRecurrente`, con un campo `periodo`) que forman parte del patrón Strategy. JPA no tiene una forma nativa de mapear un `@Embeddable` polimórfico (el `@Inheritance` con `SINGLE_TABLE`/`JOINED`/`TABLE_PER_CLASS` que usamos en `Donante` solo aplica a `@Entity`, no a tipos embebidos). La solución práctica es aplanar a mano en la tabla `necesidad`:

- `tipo_necesidad : VARCHAR(20)` — qué implementación corresponde (`RECURRENTE` / `EXTRAORDINARIA`), se reconstruye el objeto correcto al leer.
- `periodo : VARCHAR(20)` — solo tiene sentido cuando `tipo_necesidad = RECURRENTE`, `NULL` en el otro caso.

> **Nota aparte** (hallazgo de código, no de persistencia): `EntidadBeneficiariaServiceImpl.toTipoNecesidad()` siempre construye `new NecesidadRecurrente()` sin argumentos — no existe ningún campo en `NecesidadRequest` para mandar el `periodo`. Es decir, `periodo` **nunca se completa** desde la API hoy. Como `NecesidadRecurrente.estaSatisfecha()` depende de `periodo` para filtrar donaciones, en la práctica una necesidad recurrente nunca se puede marcar como satisfecha (salvo que `cantidadRequerida` sea 0). Ver entrada correspondiente en [ErroresACorregir.md](ErroresACorregir.md).

`Necesidad.donacionesAsignadas` (`List<Donacion>`) es una relación entre dos entidades reales con `id` propio — se resuelve con una tabla de unión simple (`necesidad_donaciones_asignadas`), sin necesidad de `@Embeddable`.

## Decisión: `ResultadoMatchmaking` → `@Entity`

Entidad real: `id`, `ResultadoMatchmakingRepository`, y `PUT /api/matchmaking/propuestas/{id}/estado` direcciona explícitamente por su `id`. `entidadesSugeridas` (`List<EntidadBeneficiaria>`) es, igual que `donacionesAsignadas` en `Necesidad`, una relación entre dos entidades reales → tabla de unión simple.

## Decisión: `ComprobanteEntrega` → no se persiste (mismo motivo que `RegistroDonacion`)

Solo existe dentro de `EventoEntregaExitosa`, un evento transitorio (`GestorDeEventos` lo construye al vuelo y lo emite; los eventos, como se estableció al principio del proceso, no se persisten). No hay repositorio, no tiene `id`, nunca se adjunta a `Donacion` ni a ninguna otra entidad. Igual que `RegistroDonacion`: no se mapea ni como `@Entity` ni como `@Embeddable`.

## Microservicio Logística

### Decisión: `Chofer` → `@Embeddable`

Sin `id` propio ni en el dominio ni en `ChoferRequest`/`ChoferResponse`, sin ningún `ChoferController` — siempre viaja embebido dentro de `Camion` (y, ver más abajo, duplicado dentro de `Ruta`).

### Decisión: `Camion` → `@Entity`

Entidad real: `id`, `CamionRepository`, CRUD completo en `CamionController`. `Ubicacion` embebida (ver Convenciones generales — solo la última posición conocida, sin historial).

### Decisión: `Ruta` → `@Entity`

Entidad real: `id`, `RutaRepository`, CRUD completo en `RutaController`. `Ruta.chofer` se mapea igual que en `Camion` (embebido) por consistencia de tipo, aunque — ver [ErroresACorregir.md](ErroresACorregir.md) — nunca se completa en ningún flujo real; el chofer efectivo siempre se lee a través de `camion_id`.

### Decisión: `Parada` → `@Entity` (no `@Embeddable`, pese a no tener id expuesto en la API)

Este es el caso que menciona la cátedra de "el framework te obliga a un cambio": en la API, una `Parada` se direcciona dentro de su ruta por `orden` (`POST /api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones` en realidad hace `match` contra `parada.getOrden()`, ver `RutaServiceImpl.confirmarEntregaExitosa()`), no por un id propio — a primera vista, candidata a `@Embeddable`/`@ElementCollection` igual que `Bien`.

El problema técnico: `Parada.donacionIds` es una `List<Long>` propia de cada parada. JPA no soporta de forma portable un `@Embeddable` que a su vez contenga una `@ElementCollection` cuando ese `@Embeddable` ya es el tipo de otra `@ElementCollection` (colección-dentro-de-colección). Por eso `Parada` pasa a ser `@Entity` real, con `id` autogenerado (uso interno de persistencia, no cambia el contrato de la API que sigue direccionando por `orden`), y `donacionIds` se resuelve con su propio `@ElementCollection` (`parada_donaciones`) colgando de ese id.

`entidad_id` (en `Parada`) y `donacion_id` (en `parada_donaciones`) son referencias a entidades de **otro microservicio** (`donaciones-service`): son columnas `BIGINT` simples, sin `FK` real a nivel de base de datos — consistente con la convención de que ningún esquema referencia a otro directamente.
