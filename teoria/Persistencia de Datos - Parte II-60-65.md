## PATRÓN REPOSITORIO

- Propone que en la capa de persistencia existan objetos “Repository” .

- Cada Repositorio debería poder:

- agregar(unObjeto)

- modificar(unObjeto)

- eliminar(unObjeto)

- buscar

- buscarTodos


## PATRÓN REPOSITORIO

Gráficamente, se podría representar de la siguiente forma…


## PATRÓN REPOSITORIO

- El patrón repositorio puede implementarse haciendo uso de los objetos DAOs.

- DAO es la abreviatura de Data Access Object

- Los DAO son los responsables reales de acceder a los datos. Pueden acceder a una base de datos relacional, a una base de datos no relacional, a la memoria, a archivos, etc.


## PATRÓN REPOSITORIO

- El patrón repositorio puede combinarse con el patrón de diseño Strategy para utilizar los DAOs.

- Cada repositorio podría delegar su responsabilidad en un DAO concreto.

- Para clientes de los repositorios, es indistinto el DAO concreto que se utiliza. En otras palabras, los clientes de los repositorios no deberían enterarse cuál es el medio persistente.


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
