## BASE DE DATOS RELACIONAL

## Clave Primaria

Atributo que identifica de manera unívoca a cada fila de la tabla. Existen de 3 tipos: Naturales, Subrogadas o Compuestas.


## BASE DE DATOS RELACIONAL

## Índices

Es una estructura de datos que mejora la velocidad de las operaciones, por medio de un identificador único de cada fila de una tabla. Permite un rápido acceso a los registros de una tabla en una base de datos.


## BASE DE DATOS RELACIONAL

## Clave Foránea

Es un grupo de una o más columnas en una tabla que referencian a la clave primaria de otra tabla. Una Foreign Key puede ser parte de una Primary Key.

Tanto las PK como las FK son índices


## BASE DE DATOS RELACIONAL

## Integridad Referencial

La integridad referencial es una propiedad que establece que la clave externa de una tabla de referencia siempre debe corresponder a una fila válida de la tabla a la que se haga referencia. La integridad referencial garantiza que la relación entre dos tablas permanezca sincronizada durante las operaciones de actualización y eliminación.


## BASE DE DATOS RELACIONAL

## Tipos de Datos más utilizados

- VARCHAR: Cadenas de caracteres compuestas por letras, números y caracteres especiales

- INTEGER: Números enteros naturales

- DOUBLE: Valores numéricos con punto flotante

- DATETIME: Formato para manejo de fechas


## BASE DE DATOS RELACIONAL

## Constraints

Son restricciones al modelo que se utilizan para limitar el tipo de dato que puede ingresarse en una tabla. Se especifican cuando la tabla se crea por primera vez, o posteriormente realizando una actualización.


## BASE DE DATOS RELACIONAL

## Constraints más comunes

- Not Null: No acepta valores nulos

- Unique: No acepta valores repetidos

- Check (o enum): Verifica que los valores cumplan determinada condición

- Primary Key: Clave Primaria

- Foreign Key: Clave Foránea


## BASE DE DATOS RELACIONAL

## Relaciones

- Son asociaciones entre tablas.

- Se describen en la estructura de la Base de Datos.

- Las relaciones describen cómo se vinculan una o más tablas entre sí.


## BASE DE DATOS RELACIONAL

## Cardinalidad y Modalidad

- La cardinalidad es la cantidad máxima de relaciones que tiene un registro de una entidad (tabla) con respecto a la otra tabla.

- La modalidad es la cantidad mínima de relaciones que tiene un registro de una entidad (tabla) con respecto a la otra tabla.


## BASE DE DATOS RELACIONAL

Cardinalidad y Modalidad


## BDR – ONE TO ONE

## Relación One to One

- Es una relación entre dos entidades (tablas) A y B, en la cual un registro de la entidad A se corresponde con un único registro de la entidad B.

- La relación puede ser unidireccional, en cualquier sentido, o bidireccional.


## BDR – ONE TO ONE

## Relación One to One

- Unidireccional desde el lado A: existe un registro en la tabla B que está siendo referenciado por un único registro de la tabla A.


## BDR – ONE TO ONE

## Relación One to One

- Unidireccional desde el lado B: existe un registro en la tabla A que está siendo referenciado por un único registro de la tabla B.


## BDR – ONE TO ONE

## Relación One to One

- Bidireccional: existe un registro en la tabla A que está siendo referenciado por un único registro en la tabla B; y a su vez, este registro de la tabla B está siendo referenciado por el registro de la tabla A.


## BDR – ONE TO MANY

## Relación One to Many

- Es una relación entre dos entidades (tablas) A y B en la cual un registro de la entidad A está siendo referenciado por muchos registros de la entidad B.

- Significa que “A tiene muchos B”, porque cada registro de la tabla B pertenece a un registro de la tabla A.


## BDR – ONE TO MANY

Relación One to Many

“Un alumno puede tener muchos contactos”


## BDR – MANY TO ONE

## Relación Many to One

- Es una relación entre dos entidades (tablas) A y B en la cual muchos registros de la entidad A están referenciado al mismo registro de la entidad B.

- Significa que “A pertenece a un B, y B tiene muchos A”, porque cada registro de la tabla A referencia a un registro de la tabla B, pero B puede ser referenciado muchas veces.

- También se puede leer como “Muchos A pertenecen al mismo B”


## BDR – MANY TO ONE VS ONE TO MANY

## Many to One vs One to Many

Si se tienen en cuenta dos entidades A y B podríamos afirmar que:

- Si A tiene una relación One to Many con la entidad B, entonces B tiene una relación Many to One contra la entidad A.

- Si A tiene una relación Many to One contra la entidad B, entonces B tiene una relación One to Many contra la entidad A.


## BDR – MANY TO ONE VS ONE TO MANY

Many to One vs One to Many

La relación entre las entidades A y B, en este caso, pueden llamarse de una u otra forma dependiendo el “lado” que miremos. En otras palabras, es una cuestión de perspectiva.

Regla práctica: si existe una relación One to Many, visto desde A hacia B, entonces “del otro lado” existe una relación Many to One (visto desde B hacia A). La inversa también es válida.


## BDR – MANY TO ONE VS ONE TO MANY

Many to One vs One to Many

“Un contacto pertenece a un alumno, y un alumno puede tener

muchos contactos”.

“Muchos contactos pueden pertenecer al mismo alumno”

|   | INTEGER(11) | id | INTEGER(11) |
| --- | --- | --- | --- |
| apellido | VARCHAR(50) | dato | VARCHAR(50) |
|   |   | tipo | VARCHAR(10) |
|   |   | alumno_id |   |


## BDR – MANY TO MANY

## Relación Many to Many

- Es una relación entre dos entidades (tablas) A y B en la cual un registro de la entidad A puede estar siendo apuntado por muchos registros de la entidad B, y un registro de la entidad B puede estar siendo apuntado por muchos registros de la entidad A.

- Significa que “A tiene muchos B, y B tiene muchos A”.


## BDR – MANY TO MANY

Relación Many to Many

- Esta relación no es posible realizarla en el modelo relacional, ya que sería necesario tener múltiples FKs (sin saber el número exacto) en ambas tablas involucradas en la relación.


## BDR – MANY TO MANY

## Relación Many to Many

- Para poder implementar esta relación se debe partir la misma en dos relaciones One To Many.

- La entidad A debe tener una relación One to Many contra la entidad C, considerada “entidad intermedia”; al igual que la entidad B.


## BDR – MANY TO MANY

## Relación Many to Many

- La entidad C, famosa por ser “la tabla intermedia”, debe poseer una FK a la entidad A y una FK a la entidad B.

- Además, la entidad C podría guardar algún dato extra necesario de la relación.


## BDR – MANY TO MANY

Relación Many to Many

“Un alumno puede estar cursando varias materias, y una materia

puede ser cursada por muchos alumnos”


## BDR – MANY TO MANY

## Relación Many to Many

- En el caso anterior, suponemos que no interesa guardar ningún dato extra sobre las materias que está cursando un alumno en particular.

- Si el dominio lo amerita y necesitamos guardar algún dato extra, como las notas del primer y segundo parcial, el diseño se podría mejorar y “alumno_materia” dejaría de ser una simple “tabla intermedia”.


## BDR – MANY TO MANY

Relación Many to Many


- Design the infrastructure persistence layer – Microsoft – En línea [Artículo] [URL 🔗](https://docs.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/infrastructure-persistence-layer-design)

- Diseño de Datos: Modelo Relacional – Zaffaroni Juan – En línea [Documento] [URL 🔗](https://docs.google.com/document/d/1uF3yoYIFmLxTH5ZJoT9I3cc5TW9b-H3BqZJbLudKBcA/edit)

- Diseño de Datos: Mapeo Objetos Relacional – Dodino Fernando, Bulgarelli Franco – En línea [Documento] [URL 🔗](https://docs.google.com/document/d/1YLmp9vMnSzKg2emt3Bx564Tf1CLalShPc98Z8nCoi7s/edit)

- Guía de persistencia de JPA – Bulgarelli Franco, Prieto Gastón – En línea [Documento] [URL 🔗](https://docs.google.com/document/d/1jWtehhVCFYECKvpdcCxnEgWZFCv2fR2WPyUJSoiX3II/edit#heading=h.r09lefmcufkn)

- Hibernate Inheritance Mapping – Baeldung – En línea [Sitio Web] [URL 🔗](https://www.baeldung.com/hibernate-inheritance)

## BIBLIOGRAFÍA

- Hibernate Second Level Cache – Baeldung – En línea – [Sitio Web] [URL 🔗](https://www.baeldung.com/hibernate-second-level-cache)

- Introducción a las bases NoSQL – Dodino Fernando, Tesone Pablo, Bulgarelli Franco – En línea [Documento] [URL 🔗](https://docs.google.com/document/d/1tyuJNCCsMkv4qa7yCHO69lP1cReJ0HZeT2zGfWhqinQ/edit)

- Introducción a los Sistemas de Base de Datos, C.J Date, Edit: Pearson, 2001.

- Overview of JPA/Hibernate Cascade Types – Baeldung - En línea [Sitio Web] [URL 🔗](https://www.baeldung.com/jpa-cascade-types)
