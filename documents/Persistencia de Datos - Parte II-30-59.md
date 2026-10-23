## HIDRATACIÓN LAZY VS EAGER

Cuando el ORM está realizando el proceso de Hidratación debe prestar atención a si debe, o no, popular un determinado atributo.

- Si el atributo está marcado como “eager”, entonces lo seteará.

- Si el atributo está marcado como “lazy”, entonces no lo seteará.


## HIDRATACIÓN LAZY VS EAGER

En el caso de que un atributo esté marcado como “lazy”, éste solamente será populado por el ORM, de forma transparente para el desarrollador y usuario, cuando sea llamado explícitamente.


## HIDRATACIÓN LAZY VS EAGER

- Lazy loading realiza la carga en memoria de los objetos sólo al momento de su utilización.

- Eager Loading realiza la carga en memoria de los objetos independientemente de si van a ser utilizados o no.


## ARQUITECTURAS DE ORM

Los ORM se clasifican en dos tipos de arquitecturas…


Active Record

Data Mapper


## ARQUITECTURAS DE ORM

## Active record

En esta arquitectura los ORMs decoran las clases de entidades de dominio agregándoles funcionalidades/responsabilidades. Estas responsabilidades están relacionadas a las acciones de Alta (save), Baja (delete) y Modificación (update) de la entidad, así como también las funcionalidades para permitir la búsqueda de dicha entidad (find, findBy, findAll, entre otras).


## ARQUITECTURAS DE ORM

## Active record


## ARQUITECTURAS DE ORM

## Data mapper

- En esta arquitectura los ORMs agregan un nuevo componente intermedio entre la Base de Datos y las entidades de dominio.

- Este componente se llama “entity manager” y lleva la responsabilidad de buscar, agregar, modificar y eliminar (en otras operaciones) objetos de las entidades persistentes.


## ARQUITECTURAS DE ORM

## Data mapper


## MÉTODOS DE CASCADA

“Las relaciones entre entidades, a menudo, dependen de la existencia de otra entidad. Por ejemplo, la relación Persona – Dirección, suponiendo que una dirección le pertenece siempre a una persona. Sin la Persona , la entidad Dirección no tiene ningún significado propio. Cuando eliminamos la entidad Persona , nuestra entidad Dirección también debería eliminarse.”


## MÉTODOS DE CASCADA

“La cascada implica que, cuando realicemos alguna acción en la entidad objetivo, la misma acción se aplicará a la entidad asociada.”


## MÉTODOS DE CASCADA

Existen varios tipos de Cascadas (no todos disponibles en todos los ORMs):

- ALL

- PERSIST

- MERGE

- REMOVE

- REFRESH

- DETACH


## MÉTODOS DE CASCADA

## Tipo de cascada “ALL”

- Este tipo de cascada propagará todas las operaciones desde la entidad principal a la entidad secundaria.


## MÉTODOS DE CASCADA

## Tipo de cascada “PERSIST”

- Este tipo de cascada propagará la operación de persistencia de una entidad principal a una entidad secundaria.

- Cuando guardemos la entidad principal, la entidad secundaria también se guardará.


## MÉTODOS DE CASCADA

## Tipo de cascada “MERGE”

- Este tipo de cascada propagará la operación de actualización de una entidad principal a una entidad secundaria.

- Cuando actualicemos la entidad principal, la entidad secundaria también se actualizará.


## MÉTODOS DE CASCADA

## Tipo de cascada “REMOVE”

- Este tipo de cascada propagará la operación de eliminación de una entidad principal a una entidad secundaria.

- Cuando eliminemos la entidad principal, la entidad secundaria también se eliminará.


## MÉTODOS DE CASCADA

## Tipo de cascada “REFRESH”

- La operación “refresh” refresca todos los atributos de un objeto, es decir, “re-popula” la entidad.

- Este tipo de cascada propagará la operación “refresh” de una entidad principal a una entidad secundaria.

- Cuando la entidad principal sea refrescada, la entidad secundaria también lo será.


## MÉTODOS DE CASCADA

## Tipo de cascada “DETACH”

- La operación “detach” quita la entidad del contexto persistente.

- Este tipo de cascada propagará la operación “detach” de una entidad principal a una entidad secundaria.

- Cuando la entidad principal sea quitada del contexto persistente, la entidad secundaria también lo será.


## CACHE

- Los ORM tienen la capacidad de almacenar en cache, de forma transparente, los datos recuperados desde el medio persistente.

- Esto ayuda a reducir los costos de acceso al medio persistente, en cuestiones de tiempo y recursos, para aquellos datos que son consultados con una alta frecuencia.


## CACHE

- Existen dos tipos de caches en los ORM:

- Cache de Primer Nivel

- Cache de Segundo Nivel (no todos los ORM cuentan con él)


## CACHE DE PRIMER NIVEL

- La cache de primer nivel tiene un alcance de sesión que garantiza que cada instancia de una entidad se cargue solamente una vez en el contexto persistente.

- Una vez que se cierra la sesión, la cache de primer nivel termina.

- Esta cache permite que las sesiones concurrentes trabajen con instancias de forma aislada.


## CACHE DE SEGUNDO NIVEL

- La cache de segundo nivel tiene un alcance de SessionFactory (en Hibernate), lo que significa que es compartida por todas las sesiones creadas con la misma factory.


## CACHE DE SEGUNDO NIVEL

Cuando se busca una instancia de una entidad por su id y la cache de segundo nivel está habilitada, sucede alguno de los siguientes escenarios:

- Si la instancia ya está presente en la cache de primer nivel, es devuelta desde allí.

- Si la instancia no está presente en la cache de primer nivel pero el estado de la misma está almacenado en la cache de segundo nivel, entonces se obtienen los datos desde allí, se hidrata el objeto y se devuelve la instancia.


## CACHE DE SEGUNDO NIVEL

- Si no se encuentra en ninguno de los anteriores casos, entonces se realiza el proceso de búsqueda e hidratación común y corriente.


## CACHE – ESTRATEGIAS DE CONCURRENCIA

Existen varias estrategias de concurrencia de cache de segundo nivel:

- READ_ONLY: se debería utilizar con entidades que nunca cambian. Muy adecuado

- NONSTRICT_READ_WRITE: la cache se actualiza después de que se haya confirmado una transacción que cambió los datos afectados. Existe una pequeña ventana de tiempo en la que se pueden obtener datos obsoletos desde la cache. Esta estrategia es adecuada si se puede tolerar una mínima inconsistencia eventual.


## CACHE – ESTRATEGIAS DE CONCURRENCIA

- READ_WRITE: esta estrategia garantiza una fuerte consistencia que se logra mediante la utilización de bloqueos “suaves”. Cuando se actualiza una entidad en la cache, también se almacena un bloqueo suave para esa entidad en la cache, que se libera después de que se confirma la transacción. Todas las transacciones concurrentes que acceden a registros con bloqueo suave, obtendrán los datos correspondientes directamente desde el medio persistente.


## CACHE – REPRESENTACIÓN INTERNA

- Las entidades no se almacenan en la cache como objetos, sino que solamente su guarda su estado (valores de los atributos).

- Los atributos transitorios no se guardan.

- Las colecciones no se guardan (a menos que se haya explicitado lo contrario)

- En las relaciones xToOne solamente se almacena el id de la entidad externa.


PATRÓN REPOSITORIO


## PATRÓN REPOSITORIO

- No es un patrón de Diseño

- Es un patrón arquitectónico

- Nos ayuda a estructurar el aplicativo para lograr una buena separación de concerns


## PATRÓN REPOSITORIO

- El patrón repositorio se apoya sobre el estilo en capas para estructurar el aplicativo.

- Propone crear una capa de persistencia para que la misma se encargue del acceso a los datos.
