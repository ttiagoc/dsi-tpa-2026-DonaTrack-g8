Persistencia en medios relacionales mediante

# PERSISTENCIA DE DATOS

ORM


## TEMARIO GENERAL


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Bases de Datos Relacionales

Retomando desde el punto que desembocó en la anterior explicación…

El estado de un Sistema puede ser persistido en una, o varias, Base de Datos Relacional.


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Bases de Datos Relacionales

La persistencia de un Sistema que está pensado y escrito bajo el Paradigma Orientado a Objetos, en una Base de Datos Relacional no es directa.


MAPEO OBJETO - RELACIONAL


El Mapeo Objeto-Relacional es un técnica usada para convertir los tipos de datos con los que trabaja un lenguaje orientado a objetos a tipos de dato con los que trabaja un sistema de base de datos relacional.

Mientras que ORM es el nombre de la técnica, también se suele conocer como ORMs a los frameworks que la implementan.

MAPEO OBJETO - RELACIONAL


Estas herramientas introducen una capa de abstracción entre la base de datos y el desarrollador, lo que evita que el desarrollador tenga que escribir consultas “a mano” para recuperar, insertar, actualizar o eliminar datos en la base.

MAPEO OBJETO - RELACIONAL


Al tratar de encontrar una correspondencia entre estos “mundos”, sucede que existen características que son difíciles de imitar.

A este “desajuste” se lo conoce como Impedance Mismatch

MAPEO OBJETO - RELACIONAL


## IMPEDANCE MISMATCH - IDENTIDAD

- Un objeto cumple con la característica de Unicidad.

- Un registro, en una tabla de una base de datos relacional, necesita una identificación unívoca.


## IMPEDANCE MISMATCH - IDENTIDAD

Las claves posibles para una entidad en una BDR son:

- Clave natural: algún campo propio de la entidad que lo identifique de forma unívoca. Por ejemplo, CUIT del cliente, número de teléfono del abonado, etc.

- Clave subrogada: clave ficticia (al azar, generada automáticamente, etc.).


## CONVERSIÓN DE TIPOS DE DATOS IMPEDANCE MISMATCH –

Los tipos de datos de ambos “mundos” no tienen una correlación directa.

- String sin limitaciones de tamaño vs. el VARCHAR/CHAR que requiere definirle longitud.

- Campos numéricos respecto a la precisión de decimales, o el rango de valores máximos y mínimos; incluso cuando el dominio es “un número de 0 a 3000”

- Las fechas tienen tipos no siempre compatibles entre sí: LocalDate de Java vs. Date-Datetime-Time-tinyblob del motor.

- Booleanos

- Enumerados (como los enums de Java)


## IMPEDANCE MISMATCH – MANEJO DE LA CARDINALIDAD

- En el POO, las relaciones pueden ser unidireccionales y bidireccionales; con navegabilidad bidireccional.

- Las relaciones entre las entidades de una base de datos relacional no son bidireccionales (salvo algunos casos).


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

- En el POO existe el mecanismo de herencia, el cual nos permite reutilizar lógica de una clase y/o extender su comportamiento; pero este concepto no existe en el mundo relacional.

- ¿Qué sucedería si queremos persistir una herencia? ¿Cuántas tablas se generarían en el modelo relacional? ¿Cómo sabe el ORM cuál es la clase específica que tiene que instanciar e hidratar?


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

Existen, al menos, cuatro estrategias de mapeo de herencia:

- SINGLE TABLE: Única tabla.

- JOINED: Una tabla por la superclase y una tabla por cada una de las clases hijas.

- TABLE PER CLASS: Una tabla por cada clase concreta.

- MAPPED SUPERCLASS: Los atributos de la superclase son persistidos en las tablas de las clases hijas.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Mapped Superclass

- Los atributos de la superclase son persistidos en las tablas de las clases hijas; y la superclase no es considerada una Entidad.

- Generalmente utilizado cuando la superclase exhibe, únicamente, comportamiento y/o uno o “pocos” atributos en común.

- Uno de los usos más comunes suele ser cuando todas las clases persistentes tienen una PK subrogada y/o un campo “activo” (booleano); pero entre ellas no existe nada más en común.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Single Table

- Suponiendo que existe una única superclase y N clases hijas, el resultado de mapear la herencia con la estrategia Single Table generará una única tabla en la base de datos.

- Esta única tabla contendrá una columna por cada uno de los atributos persistentes de la superclase + una columna por cada atributo persistentes de cada una de las clases hijas.

- Además, tendrá una columna que actuará de “campo discriminador”, la cual indicará a qué clase pertenece la instancia/fila en cuestión.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Single Table

Si consideramos una superclase con N atributos persistentes; una clase hija A con M atributos persistentes; y otra clase hija B con P atributos persistentes; entonces:

- Si se persiste una instancia de la clase A (con todos sus atributos seteados, inclusive los pertenecientes a la superclase), quedarán P columnas nulas.

- Si se persiste una instancia de la clase B (con todos sus atributos seteados, inclusive los pertenecientes a la superclase), quedarán M columnas nulas.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Single Table

- Como consecuencia de la utilización de esta estrategia de mapeo de herencia, varias columnas de la tabla resultante podrían ser nulas.

- Pero, en contraparte, se obtiene una buena performance ya que solamente se debe consultar una única tabla.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Joined

- Suponiendo que existe una única superclase y N clases hijas, el resultado de mapear la herencia con la estrategia Joined generará, en la base de datos, una tabla por la superclase y N tablas más, una por cada clase hija.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Joined

- La tabla que representa a la superclase contendrá, únicamente, tantas columnas como atributos persistentes existan en dicha clase. Además, puede contener opcionalmente el campo discriminador.

- Cada una de las tablas que representan a las clases hijas contendrán tantas columnas como atributos persistentes existan en dichas clases.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Joined

- Cada una de las PKs de las tablas que representan a las clases hijas, a su vez serán una FK a la tabla que representa a la superclase.

- Para recuperar un objeto con todos sus atributos (los propios + los que están en la superclase), el ORM debe joinear las tablas.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Table per Class

- Suponiendo que existe una única superclase y N clases hijas concretas, el resultado de mapear la herencia con la estrategia Table per Class generará, en la base de datos, N tablas: una por cada clase hija.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Table per Class

- Todos los atributos persistentes de la superclase serán persistidos en cada una de las tablas que mapean contra las clases hijas.


## IMPEDANCE MISMATCH – MAPEO DE LA HERENCIA

## Table per Class

- Para recuperar polimórficamente todos los objetos, el ORM debe realizar unions entre todas las tablas que mapean contra las clases hijas.


DISEÑO Y FUNCIONAMIENTO DEL ORM


¿Cómo recupera un ORM un objeto desde la Base de Datos?

Cuando el ORM recupera un objeto desde la base de datos ejecuta el proceso de Hidratación.


El proceso de Hidratación consiste en:

- 1. Instanciar la clase del objeto que se quiere recuperar (previamente habiendo recuperado los datos mínimos mediante la ejecución de una sentencia SQL).

- 2. Popular el objeto, es decir, asignarle a cada uno de sus atributos (aquellos no marcados como “lazy loading”) los valores recuperados desde la base de datos.
