# Justificaciones de Diseño Relacional — DonaTrack

Acompaña a [DER.puml](DER.puml). Para cada tabla se explica brevemente por qué se modeló de esa forma.

## Consideraciones generales

- **Claves primarias**: `BIGINT` con `GenerationType.IDENTITY` en todas las entidades, soportado de forma nativa por HSQLDB (tests) y PostgreSQL/MariaDB (despliegue). La excepción es `cambio_estado`, que usa la clave natural `(donacion_id, orden)`.
- **Un esquema por microservicio**: `notificaciones`, `donaciones` y `logistica` no comparten tablas. Las referencias a datos de otro servicio se guardan como un id simple, sin FK.
- **Value objects embebidos**: `MedioContacto`, `Ubicacion`, `Chofer`, `Representante` y `Bien` no tienen identidad propia ni se consultan por separado, por lo que no tienen tabla con PK. Si son un único valor se embeben como columnas en la tabla dueña (`@Embedded`); si son una lista van a una tabla auxiliar con FK al dueño (`@ElementCollection`).
## Esquema `notificaciones`

### `notificacion`
Entidad con identidad propia, se guarda cada notificación enviada. El `MedioContacto` de destino se embebe (`contacto_valor`, `contacto_tipo_contacto`) porque es un value object sin identidad.
Además, almacena atributos de contexto de negocio y auditoría:
- `tipo_evento`: el tipo o motivo de la notificación (por ejemplo `DONACION_RECIBIDA`, `ENTREGA_EXITOSA`, `RECORDATORIO_INACTIVIDAD`).
- `destinatario_id`: identificador lógico del destinatario (donante o entidad beneficiaria), sin FK externa para mantener el aislamiento entre microservicios.
- `donacion_id`: identificador lógico de la donación asociada si aplica, sin FK externa.

## Esquema `donaciones`

### `donante`
`Donante` es abstracta con dos subtipos (`PersonaHumana`, `PersonaJuridica`). Se eligió herencia **`SINGLE_TABLE`** con la columna discriminadora `tipo_donante`, porque las consultas más frecuentes son polimórficas (listar todos los donantes, buscar uno por id sin conocer su tipo) y esta estrategia las resuelve sin JOIN ni UNION. Es una desnormalización deliberada: las columnas propias de un subtipo quedan en `NULL` para el otro, así que no pueden ser `NOT NULL` y su obligatoriedad se valida en la aplicación, en dos niveles. Los constructores de las entidades exigen lo que todo donante debe tener, venga del alta o de la importación CSV: al menos un medio de contacto, uno de ellos email, un contacto predeterminado que sea uno de esos medios, y nombre y DNI (humana) o razón social y CUIT (jurídica). El servicio de alta exige además el resto de los datos de registro (apellido, fecha de nacimiento, género y dirección; rubro, tipo y representantes), que el CSV no trae. `JOINED` requería un JOIN en cada lectura y `TABLE_PER_CLASS` un UNION para listar todos. El `contactoPredeterminado` se embebe.

### `donante_contactos`
Lista de `MedioContacto` del donante. Al ser value objects sin identidad, se guardan en una tabla auxiliar con FK al donante, sin PK propia.

### `donante_representantes`
Representantes de una persona jurídica. No tienen id ni se consultan o editan por separado, siempre se manejan junto al donante, así que van en una tabla auxiliar con FK al donante. Su correo (`MedioContacto`) se aplana en columnas.

### `categoria`
Catálogo de categorías con `nombre` único, cargado previamente desde su propio endpoint. Es una tabla propia, y no una copia dentro de cada bien, para que todas las referencias compartan los mismos valores de `pide_estado` y `es_perecedero`.

### `subcategoria`
Catálogo de subcategorías con FK a su categoría. Es la unidad mínima de asignación del sistema y la referencian `donacion`, `donacion_bienes` y `necesidad`, por lo que debe existir una única vez.

### `registro_donacion`
Representa la carga completa de bienes que hace un donante en el depósito, antes de segmentarse. Se persiste para:
- conservar la **descripción general** de la donación que pide la consigna;
- saber qué donaciones salieron de una misma carga (**trazabilidad**);
- obtener la **fecha de la última donación** de cada donante, que usa el proceso de detección de inactividad.

Sus bienes no se guardan acá: después de segmentar quedan en `donacion_bienes`, y guardarlos también en esta tabla sería duplicarlos.

### `donacion`
Entidad central del sistema, con id propio y su propio ABM. Cada registro genera una o más donaciones y cada donación pertenece a un único registro (`registro_donacion_id`, FK `NOT NULL`), por lo que no hace falta tabla intermedia. `entidad_beneficiaria_asignada_id` es `NULL` hasta que el matchmaking asigna una entidad.

`necesidad_id` indica qué necesidad cubre la donación. La relación es uno a muchos: una necesidad junta varias donaciones, pero cada donación es la unidad mínima de asignación y cubre como mucho una necesidad. Por eso es una FK en `donacion` y no una tabla de unión, que permitiría asignar la misma donación a varias necesidades. Es `NULL` mientras la donación no esté asignada, o si se elimina la necesidad; en ese caso la donación se conserva.

**Desnormalización deliberada**: `donante_id` y `fecha` se podrían obtener del registro por JOIN, pero se mantienen en `donacion`. Es la tabla más consultada del sistema (listados, matchmaking, notificaciones, planificación de rutas) y así esas consultas tienen toda la información sin JOIN adicional. El riesgo de inconsistencia es bajo porque ambos valores se copian del registro una única vez, al segmentar, y nunca se modifican.

### `donacion_bienes`
Bienes de cada donación. No tienen id ni se consultan por separado, siempre se leen junto a su donación, así que van en una tabla auxiliar con FK a `donacion`. La subcategoría sí es una FK real al catálogo.

### `cambio_estado`
Historial de cambios de estado de la donación, para trazabilidad y auditoría. `CambioEstado` es un value object: no tiene identidad propia, no se consulta por fuera de su donación y no existe sin ella. Por eso va en un `@ElementCollection` de la donación, en vez de ser una entidad con id propio.

Su PK es la clave natural `(donacion_id, orden)`, donde `orden` es la posición de la transición dentro del historial (`@OrderColumn`). Esta PK es necesaria: en una colección sin PK, Hibernate no puede identificar una fila y ante cada cambio de estado borra todo el historial de la donación y lo vuelve a insertar. Una tabla de auditoría no debería reescribirse. Con la PK, cada transición nueva es un único `INSERT` al final y las filas anteriores no se tocan.

No se usa `(donacion_id, fecha)` como clave porque la fecha no es única por construcción: la fecha de entrega la informa el servicio de logística y puede repetirse (por ejemplo, si reenvía la misma confirmación), y dos transiciones encadenadas pueden caer en el mismo instante. `orden` sí es único dentro de cada donación.

`patente_camion` solo se completa en la transición a `ENTREGADA`. Se guarda en la transición y no en `donacion` porque una entrega fallida se replanifica y puede haber varios intentos, cada uno con su camión.

### `donacion_fotos_recepcion`
URLs de las fotos de recepción. Es una lista de valores simples (`String`) sin identidad, así que va en una tabla auxiliar con FK a `donacion`.

### `entidad_beneficiaria`
Entidad con id propio y su propio ABM. El teléfono (`MedioContacto`) se embebe en columnas.

### `entidad_beneficiaria_correo_representantes`
Lista de correos de los representantes. Mismo criterio que `donante_contactos`: tabla auxiliar con FK a la entidad.

### `necesidad`
Entidad propia, a diferencia de `Bien`, porque se consulta y se elimina individualmente por su id. El tipo de necesidad es un Strategy (`NecesidadExtraordinaria` / `NecesidadRecurrente`), no herencia de entidades, por lo que se aplana en dos columnas: `tipo_necesidad`, que indica cuál corresponde, y `periodo`, que solo se usa si es recurrente. Sus donaciones asignadas se vinculan con la FK `donacion.necesidad_id` (ver `donacion`).

### `resultado_matchmaking`
Entidad propia: cada propuesta del matchmaking se consulta y se acepta o rechaza por su id. Tiene FK a la donación evaluada.

### `resultado_matchmaking_entidades_sugeridas`
Relación muchos a muchos entre el resultado y las entidades sugeridas. Ambas son entidades con id propio, así que se resuelve con una tabla de unión.

### Clases que no se persisten
`ComprobanteEntrega` solo existe dentro del evento de entrega exitosa, que se emite y se descarta. No tiene id ni se consulta después, así que no tiene tabla.

## Esquema `logistica`

### `camion`
Entidad con id propio y su propio ABM. El `Chofer` se embebe porque no tiene id ni se gestiona por separado, siempre está asignado a un camión. La `Ubicacion` también se embebe, porque solo interesa la última posición conocida y no el historial del recorrido.

### `ruta`
Entidad con id propio y su propio ABM, con FK al camión asignado.
Adicionalmente, guarda un snapshot histórico embebido del chofer (`chofer_nombre`, `chofer_apellido`) al momento de planificarse o iniciarse el recorrido. Esto resuelve el problema de rotación de choferes en el camión: si un camión cambia de chofer asignado en el futuro, las rutas ejecutadas en el pasado preservan la identidad fidedigna de la persona que condujo el vehículo en esa fecha.

### `parada`
Se modela como entidad con id propio: la API confirma o reporta fallas de entrega de una parada puntual por su id, y además cada parada tiene su propia lista de ids de donaciones, algo que JPA no permite de forma portable si la parada fuera un elemento embebido dentro de la colección de la ruta. `entidad_id` referencia a una entidad beneficiaria de otro microservicio, así que es un id simple sin FK.
Dispone de una columna `estado` (`PENDIENTE`, `ENTREGADA`, `NO_RECIBIDA`) que permite conocer el progreso y resultado de cada parada individualmente a lo largo de la ruta, sin depender únicamente del estado global de la `ruta`.

### `parada_donaciones`
Ids de las donaciones a entregar en cada parada, en una tabla auxiliar con FK a `parada`. `donacion_id` pertenece al esquema `donaciones`, así que no tiene FK real.
