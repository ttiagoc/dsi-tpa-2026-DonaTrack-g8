Persistencia en medios relacionales mediante

# PERSISTENCIA DE DATOS

ORM


## TEMARIO GENERAL


## PERSISTENCIA

En términos informáticos, la persistencia se refiere a que el estado de un sistema sobrevive más allá del proceso que lo creó; o dicho en otras palabras, sobrevive más allá de una única ejecución.

Esto se logra, en la práctica, almacenando dicho estado en algún (o algunos) medio persistente, tal como una base de datos o archivos.


## ESTRATEGIAS DE PERSISTENCIA

Existen varias estrategias de persistencia de datos un aplicativo:

- Persistencia en Memoria

- Persistencia en Archivos

- Prevalencia

- Ortogonal

- Persistencia en Base de datos

- Bases de datos de Objetos

- Bases de datos No-SQL

- Bases de datos Relacionales


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Memoria

- “La persistencia en memoria es la capacidad de un dato u objeto de seguir existiendo luego de ser utilizado en distintas operaciones.”

- Si bien la memoria RAM es un medio volátil, lo que se contradice con el concepto de persistencia puro; existen, también, memorias persistentes. [URL 🔗](https://docs.microsoft.com/es-es/azure-stack/hci/concepts/deploy-persistent-memory)


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Memoria

- Este tipo de persistencia es utilizado mayormente para realizar tests en los sistemas.

- También existen implementaciones de Bases de Datos que persisten sus datos en memoria, tal como SQLite. [URL 🔗](https://www.sqlite.org/inmemorydb.html)


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Archivos

La persistencia en Archivos involucra guardar el estado de un Sistema en uno o varios Archivos para que luego éste pueda ser

recuperado,

funcionando/ejecutando desde el mismo punto, es decir, como “si nada hubiera pasado”.

y el Sistema pueda continuar


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Archivos

Existen varios tipos de Archivos/Formatos de intercambio que son utilizados para persistir o transmitir datos, tales como:

- XML

- CSV

- Ancho Fijo

- JSON

- Otros…


## ESTRATEGIAS DE PERSISTENCIA

## Prevalencia

Es una técnica que almacena el Estado de un Sistema en la Memoria principal, pero que regularmente genera “snapshots” a

disco para evitar la pérdida completa de los datos. Las implementaciones de esta técnica tienen la capacidad de

manejar transacciones.


## ESTRATEGIAS DE PERSISTENCIA

## Prevalencia

Una de las principales ventajas es que no existen transformaciones de datos, ya que éstos son persistidos en el formato que el aplicativo utiliza o “entiende”. Como consecuencia se obtiene un alto grado de transparencia y una mejora notable en la performance.


## ESTRATEGIAS DE PERSISTENCIA

## Prevalencia

Como desventaja se destaca la falta de interoperabilidad de los datos, además de que debe considerarse una alta capacidad de Memoria ya que la misma debe poder alojar al aplicativo completo.


## ESTRATEGIAS DE PERSISTENCIA

Prevalencia

Prevayler es una posible implementación para Java. [URL 🔗](https://prevayler.org/)


## ESTRATEGIAS DE PERSISTENCIA

## Ortogonal

La persistencia Ortogonal refiere a que la Persistencia del Estado de un Sistema se implementa como una propiedad intrínseca de su entorno de ejecución.

Por este motivo, no se requieren acciones específicas para poder guardar o recuperar datos, ya que esto ocurre de forma “transparente”.


## ESTRATEGIAS DE PERSISTENCIA

## Ortogonal

Este tipo de persistencia es adoptado por los Sistemas Operativos, permitiendo funcionalidades como la Hibernación; y en Sistemas de Virtualización de Plataformas como VMware o VirtualBox.


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Base de Datos

- En este caso, el Estado de un Sistema es persistido en una (o varias) Base de Datos.

- Los datos persistentes del aplicativo, dependiendo el tipo de Base de Datos, necesitan una transformación antes de ser persistidos.


## ESTRATEGIAS DE PERSISTENCIA

## Persistencia en Base de Datos

- La interoperabilidad es un atributo de calidad que se maximiza en este caso, ya que los datos se persisten de forma independiente a los Sistemas que los manipulan.

- Las bases de datos otorgan mecanismos para recuperarse en caso de fallos, además de brindar reglas para resguardar la integridad de los datos.


## ESTRATEGIAS DE PERSISTENCIA

Persistencia en Base de Datos Orientadas a Objetos

- Cada una de las Bases de Datos Orientadas a Objetos está atada a un lenguaje de programación en particular, ya que en cada lenguaje los objetos tiene una representación interna diferente.

- Como ventaja se destaca la no transformación de los datos, cuya característica proporciona una mayor performance.


## ESTRATEGIAS DE PERSISTENCIA

Persistencia en Base de Datos Orientadas a Objetos

- La principal desventaja radica en la interoperabilidad de los datos.


## ESTRATEGIAS DE PERSISTENCIA

Persistencia en Base de Datos Relacionales y No-SQL

Antes de entrar en esta sección, es necesario conocer Teoría sobre el Modelo Relacional.


MODELO RELACIONAL


## MODELO RELACIONAL

- Fue postulado por Edgar Frank Codd (IBM) en 1970. [URL 🔗](https://www.ibm.com/ibm/history/exhibits/builders/builders_codd.html)

- Surge como un nueva forma de organizar los datos.

- Todos los datos son almacenados en relaciones. Cada relación es un conjunto de datos organizados, llamados tuplas.

- Facilita la organización de grandes volúmenes de datos y garantiza la integridad de los mismos.


## MODELO RELACIONAL

- Relación: es la “entidad” donde se almacenan los datos.

- Tupla: es el registro.

- Atributo: es un Campo dentro de la Tupla.


## MODELO RELACIONAL

- Dominio: es el conjunto de valores posibles que puede tomar un Atributo. Es la menor unidad semántica de información.

- Cardinalidad: número de tuplas.

- Grado: número de Atributos.

- Clave Primaria: es el identificador único de cada tupla.


## MODELO RELACIONAL


## BASE DE DATOS RELACIONAL

- Es un tipo de Base de Datos que cumple con el Modelo Relacional.

- En una BDR, todos los datos se almacenan y se accede a ellos por medio de relaciones previamente establecidas.

- Las relaciones que almacenan datos son llamadas “relaciones base” y su implementación es llamada “tabla”.


## BASE DE DATOS RELACIONAL

En una BDR los datos están organizados en relaciones llamadas Tablas, donde cada tupla es representada por una Fila, que a su vez contiene múltiples Columnas (atributos), y donde cada celda puede tomar alguno de los valores definidos en su dominio.
