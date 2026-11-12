# Justificaciones de Diseño — DonaTrack

Acompaña a [Clases.puml](Clases.puml), [CasosDeUso.puml](CasosDeUso.puml) y [Despliegue.puml](Despliegue.puml).
Las decisiones de temas puntuales están en documentos aparte:

- Modelo de datos: [JustificacionesDisenoRelacional.md](JustificacionesDisenoRelacional.md) (acompaña a [DER.puml](DER.puml)).
- Autenticación y autorización: [JustificacionAutenticacion.md](JustificacionAutenticacion.md).
- Sitio web: [JustificacionWebService.md](JustificacionWebService.md).
- Comunicación entre logística y donaciones: [JustificacionGrpc.md](JustificacionGrpc.md).

---

## 1. Casos de uso

**Iniciar sesión.** No está en el diagrama general para que se lea rápido. Es una precondición de todos los casos de
uso de donante, entidad, chofer y administrador.

**Registrar donante y registrar entidad beneficiaria.** Son casos de uso distintos y no un único "Registrar usuario":
los datos no tienen nada en común. Del donante interesan el DNI, el género o los datos de su organización; de la
entidad (una escuela, un comedor), sus datos y sus representantes.

**Exponer notificación.** La lógica de los avisos se separa en este caso de uso, que incluyen "Importar donantes",
"Registrar donante" y "Registrar entidad beneficiaria". Se conecta con el actor secundario "Servicio externo de
notificación", que representa a los proveedores de email, SMS y WhatsApp.

---

## 2. Modelo de dominio — Donaciones

### Donante (persona humana y jurídica)
`Donante` es una clase abstracta extendida por `PersonaHumana` y `PersonaJuridica`. Centraliza lo común (id, medios
de contacto, contacto predeterminado e historial de donaciones) y el resto del sistema trabaja con la abstracción sin
importar el tipo (principio de sustitución de Liskov).

### Representante
Es una clase aparte, en una lista dentro de `PersonaJuridica`. No hereda de `Donante` porque no es quien aporta los
bienes: es un contacto autorizado para operar en nombre de la organización. Así sus datos no se mezclan con el
historial de donaciones.

### Medio de contacto y contacto predeterminado
`Donante` tiene `contactoPredeterminado`, que es uno de sus `MedioContacto`. Para cambiar el canal por el que se le
avisa (email, SMS o WhatsApp) alcanza con cambiar esa referencia, sin tocar el resto del perfil.

### Bienes, categorías y subcategorías
Hay una única clase `Bien` con atributos opcionales (estado, fecha de vencimiento), en lugar de subclases rígidas
como `BienUsado` o `BienPerecedero`. Es la `Categoria` la que dice qué datos aplican (`pideEstado`, `esPerecedero`):
si un dato no corresponde (el vencimiento de una silla) queda vacío, y las reglas se cambian desde el ABM de
categorías sin tocar código. Se validan al crear el bien: un perecedero sin vencimiento o un bien sin estado, cuando
la categoría lo pide, se rechazan.

Cada bien guarda su **peso y volumen por unidad**. Son obligatorios porque los usan dos cálculos: cuánto cubre una
donación de una necesidad medida en kilogramos o litros, y cuánto ocupa en un camión.

### Segmentación de donaciones
`RegistroDonacion` representa la carga completa que entra al depósito, con bienes mezclados.
`SegmentadorDeDonacion` (responsabilidad única) la divide en varias `Donacion` independientes, una por
subcategoría, vencimiento y estado. Así el matchmaking siempre trabaja con unidades mínimas de asignación.

### Estado de la donación
`Donacion` no pisa su estado: guarda una lista de `CambioEstado` (`historialEstados`) con la fecha, el estado, la
justificación (por ejemplo, el motivo de una entrega fallida) y la patente del camión en las entregas. Así se puede
reconstruir toda su historia. El estado actual también se guarda aparte (`estadoActual`) para poder filtrar por
estado sin recorrer el historial.

Las transiciones son métodos del dominio con sus reglas: `asignarA` (solo desde el depósito), `volverADeposito`,
`reprogramarEntrega` (solo una entrega fallida con entidad asignada), `confirmarEntrega` y `confirmarRecepcion` (con
las fotos que carga la entidad).

### Satisfacción de necesidades (`TipoNecesidad`)
Las necesidades extraordinarias suman todo lo asignado; las recurrentes, solo lo del período actual (diario,
semanal, mensual o anual). Es un **Strategy**: `Necesidad` delega en `TipoNecesidad` cuánto lleva asignado y si está
satisfecha, y una regla nueva se agrega sin modificar `Necesidad` (abierto/cerrado).

### Unidad de medida de una necesidad
Una necesidad se puede pedir en unidades, kilogramos o litros ("30 kg de fideos"), pero los bienes siempre se donan
en unidades con su peso y volumen por unidad. La conversión la sabe el enum `UnidadDeMedida`: `cantidadDe(bien)`
devuelve la cantidad, `cantidad × peso` o `cantidad × volumen × 1000` según el caso. Así, 5 paquetes de 5 kg son
25 kg y no cubren una necesidad de 30 kg, aunque sí cubren una de 5 unidades.

La suma se redondea a 2 decimales (`UnidadDeMedida.redondear`) porque sumar decimales en `double` deja restos
(8 × 0,1 kg da 0,7999…). Se usa el mismo valor redondeado para decidir si la necesidad está satisfecha y para
mostrarlo, así la pantalla nunca dice "30 de 30 kg" en una necesidad sin cubrir.

### Asignación de donaciones (motor de matchmaking)
`MotorDeMatchmaking` es el contexto de un **Strategy**: ejecuta una lista de `AlgoritmoAsignacion`
(`CompatibilidadSemantica`, `PrioridadSubAtendidos`) y guarda cada propuesta en un `ResultadoMatchmaking`, que el
administrador acepta (eligiendo la entidad) o rechaza. Para sumar o cambiar un criterio de ranking no hay que tocar el
motor.

### Eventos de dominio (Observer)
Para que las reglas de negocio no dependan del envío de avisos ni de otros servicios, cada servicio tiene su propio
despachador: `EventManagerDonaciones` y `EventManagerLogistica`. El dominio solo llama a `emitir(evento)` y los
listeners suscriptos reaccionan: `DonacionAsignadaListener`, `EntregaExitosaListener`, `EntregaFallidaListener`,
`RecepcionConfirmadaListener`, `AusenciaPlataformaListener` e `InicioRutaListener`.

Los listeners se ejecutan en el mismo hilo, uno detrás de otro, y cada uno atrapa sus propios errores: si el
servicio de notificaciones no responde, el aviso se pierde pero la operación de negocio (asignar, entregar,
confirmar) ya quedó hecha.

### Usuarios y acceso
`Usuario` está separado del perfil y lo referencia por `perfilId`: un `Donante`, una `EntidadBeneficiaria` o, para el
chofer, el `Camion` que maneja. Los perfiles se crean también sin usuario (importación CSV, alta en el depósito). Para esos donantes,
`AccesoInicialDonante` crea el usuario con su correo y una contraseña temporal y les manda la bienvenida con esas
credenciales. El detalle de roles, JWT y BCrypt está en [JustificacionAutenticacion.md](JustificacionAutenticacion.md).

### Importación de donantes (CSV de más de 10.000 filas)
`ImportadorDeDonantes` no procesa el archivo dentro del pedido HTTP: lo guarda, crea una `ImportacionTarea` y lo
procesa en segundo plano, leyéndolo línea por línea (`BufferedReader`) sin cargarlo entero en memoria. La tarea lleva
la cuenta de filas procesadas, creadas, actualizadas y descartadas y la lista de errores por fila, y el administrador
ve el avance en la pantalla. Así un archivo grande no corta la conexión por timeout.

### Direcciones verificadas (`Geocodificador`)
Las direcciones de las entidades se validan y se ubican con la API Georef del Estado (`GeorefGeocodificador`) al
registrarse o mudarse, y las coordenadas se guardan en la entidad (`ubicarEn`). El dominio depende de la interfaz
`Geocodificador`, no de Georef: el proveedor se puede cambiar y en los tests se reemplaza por uno falso. Si la
dirección no existe se rechaza con un mensaje claro (`DireccionInvalidaException`); si Georef no responde se informa
aparte (`GeocodificadorNoDisponibleException`), para no culpar al usuario por una falla externa.

---

## 3. Modelo de dominio — Notificaciones

**Strategy + Adapter.** `Notificador` elige la `EstrategiaNotificacion` (`Email`, `SMS`, `WhatsApp`) según el tipo
de contacto del destinatario. Cada estrategia usa un contrato propio (`ProveedorEmail`, `ProveedorSMS`,
`ProveedorWhatsapp`) implementado por un adaptador del proveedor concreto (`SmtpEmailAdapter` para el relay SMTP de
Brevo, `ResendAdapter`, `TwilioAdapter`). El dominio no depende de ningún SDK y cambiar de proveedor es cambiar de
adaptador (segregación de interfaces).

Cada notificación se guarda con su evento, el tipo y el id del destinatario (donante, entidad o administrador) y la
donación asociada, para que cada usuario vea sus avisos en la plataforma.

---

## 4. Modelo de dominio — Logística

### Entidades físicas separadas de la integración
`Camion`, `Ruta`, `Parada` y `Ubicacion` modelan la flota y el recorrido. La comunicación con otros sistemas queda en
componentes aparte, así esas clases no dependen de la red.

- **`Ruta`** guarda una copia del `Chofer` al planificarse: si el camión cambia de chofer, las rutas pasadas siguen
  diciendo quién manejó.
- **`Parada`** tiene su propio estado (`PENDIENTE`, `ENTREGADA`, `NO_RECIBIDA`) y las coordenadas de la entidad,
  copiadas al planificar. El chofer resuelve cada parada por separado y la ruta termina cuando todas están resueltas
  (`todasLasParadasResueltas`).

### Planificación de rutas: componente externo
La consigna plantea la planificación como un **servicio externo** que procesa como máximo 100 donaciones por pedido y
devuelve el resultado de forma asincrónica. El diseño lo respeta aunque en la entrega el servicio esté simulado:

- `PlanificadorDeRutas` pide a donaciones (por gRPC) las donaciones asignadas, las parte en **lotes de 100** y
  reparte los camiones disponibles entre los lotes.
- `GestorPlanificacionRutas` es el **adaptador** hacia el componente externo: le manda cada lote y el resultado
  vuelve por un **callback REST** (`POST /api/rutas/planificaciones/resultados`), que crea las rutas y paradas.
- Como no hay un proveedor real, el adaptador simula la respuesta (`calcularPlanificacion`): asigna cada donación al
  primer camión donde entra por peso y volumen, priorizando el que ya pasa por esa entidad. Las que no entran en
  ningún camión quedan para la próxima planificación.

Para usar un planificador real alcanza con cambiar el adaptador: el callback, las rutas y el resto de logística no
cambian.

### Seguimiento de camiones
`MonitorDeRutas` guarda la última `Ubicacion` de cada camión (no el historial, que no se usa) y arma la lista de
camiones activos. Con eso el donante y la entidad siguen en un mapa los camiones que llevan sus donaciones.

---

## 5. Arquitectura e integración entre servicios

### División en servicios
- **Donaciones** (principal): inventario, donantes, entidades, necesidades, matchmaking y usuarios.
- **Logística** (el servicio extraído en la entrega 2): flota, planificación, rutas y seguimiento.
- **Notificaciones**: servicio genérico de infraestructura que habla con los proveedores de mensajería.
- **Sitio web** (`web-service`): cliente liviano que arma el HTML y consume las tres APIs. No tiene base ni lógica de
  negocio (ver [JustificacionWebService.md](JustificacionWebService.md)).

Cada servicio tiene su propia base y no comparte tablas: lo que necesita de otro servicio lo guarda como un id, sin
FK.

### Cómo se comunican
| Origen → destino | Protocolo | Autenticación | Para qué |
|---|---|---|---|
| Navegador → web | HTTP | Cookie de sesión `HttpOnly` | Todas las pantallas |
| Web → donaciones, logística, notificaciones | REST/JSON | JWT del usuario (`Authorization: Bearer`) | Datos de cada pantalla |
| Logística → donaciones | **gRPC sobre HTTP/2** | Clave compartida en los metadata (`x-api-key`) | Lotes a planificar, cambios de estado, entregas y contactos ([JustificacionGrpc.md](JustificacionGrpc.md)) |
| Donaciones y logística → notificaciones | REST/JSON | — | Enviar avisos de eventos |
| Logística ↔ planificador externo | REST/JSON | El callback exige la clave compartida (`X-Api-Key`, rol `SISTEMA`) | Pedido de planificación y callback |
| Donaciones → Georef | HTTPS | — | Validar y ubicar direcciones |
| Notificaciones → Brevo / Resend / Twilio | SMTP / HTTPS | Credenciales del proveedor | Email, SMS y WhatsApp |

---

## 6. Despliegue en Azure

El sistema se publica en `https://donatrack-g8.chilecentral.cloudapp.azure.com` (Azure for Students, región Chile
Central) y se reparte en cuatro piezas dentro de la misma red virtual (VNet). Los pasos están en
[`deploy/README.md`](../deploy/README.md).

| Pieza | Qué corre |
|---|---|
| **Base de datos** (Azure Database for PostgreSQL, o una VM con PostgreSQL) | Las tres bases, una por servicio |
| **VM de servicios** (`donatrack-vm`, B2als v2) | Caddy como balanceador de carga y HTTPS, y **dos instancias** de cada servicio y del web-service ([`deploy/servicios/docker-compose.yml`](../deploy/servicios/docker-compose.yml)) |
| **VM de cron** (B2ats v2, sin IP pública) | Las tareas programadas |
| **Azure Files** | Las fotos de recepción |

**Por qué así:**
- **Cada cosa escala y falla por separado:** los datos y las fotos ya no viven en la VM de servicios, así que esa VM se
  puede rehacer o duplicar sin perder nada. La base administrada tiene copias de seguridad automáticas.
- **Dos instancias detrás de un balanceador:** Caddy reparte las requests entre las instancias (`round_robin`). Si una
  no responde, la saca de la rotación por 30 s y reintenta en la otra, así que se puede reiniciar o actualizar una
  instancia sin cortar el servicio. Solo reintenta cuando no pudo conectarse, así un `POST` nunca se ejecuta dos veces.
- **Sesiones pegajosas en el sitio:** la sesión del web-service vive en la memoria de Jetty de la instancia que logueó
  al usuario. En vez de agregar un almacén de sesiones compartido, Caddy le pone una cookie al navegador y lo manda
  siempre a la misma instancia. El costo es que, si esa instancia se cae, el usuario tiene que volver a ingresar.
- **APIs sin estado:** cada request trae su JWT o su `X-Api-Key`, así que cualquier instancia la atiende. El tráfico
  HTTP entre servicios también pasa por el balanceador (`http://caddy:8080/api`, ...). El gRPC de logística a
  donaciones va al alias `donaciones-service`, que Docker resuelve a las dos instancias.
- **Fotos compartidas sin tocar el código:** el web-service las sigue guardando en `FOTOS_DIR`, pero esa carpeta es un
  file share de Azure Files montado en la VM, así que las dos instancias ven las mismas.
- **Mismo entorno en local y en la nube:** las imágenes son las mismas que se usan con el `docker-compose.yml` de la
  raíz. Todo se configura por variables de entorno (URLs, base, secretos, `COOKIE_SECURE`).
- **Red privada:** desde Internet solo entra Caddy (80 y 443, más el 22 para SSH). La base, el cron y las APIs solo
  se ven desde la VNet. Los puertos 8080/8081 que usa el cron los cierra el NSG a Internet, y Caddy además rechaza
  cualquier IP que no sea privada.
- **HTTPS sin tocar el código:** Caddy obtiene y renueva solo el certificado de Let's Encrypt y le pasa el tráfico al
  sitio. Así la cookie de sesión puede ir con `Secure` y el web-service no maneja certificados.
- **Costo:** la base (B1ms), la VM de cron (B2ats v2) y las fotos entran en los servicios gratuitos de 12 meses de Azure.
  Lo único que consume crédito es la VM de servicios, la misma que ya se usaba. Usar Caddy como balanceador, en vez de
  un Azure Load Balancer, ahorra ese costo.

**Lo que se resigna:** las dos instancias de cada servicio están en la **misma VM**, así que esa VM sigue siendo un
punto único de falla para el procesamiento (no para los datos). Caddy también es único. Como las instancias se
hablan por red y no comparten estado, pasar a dos VMs de servicios detrás de un Azure Load Balancer solo requiere
cambiar las direcciones del Caddyfile. Las importaciones de donantes en curso se siguen en la memoria de la instancia
de donaciones que las recibió.

### Tareas programadas con el cron de la VM
La consigna pide tres procesos periódicos: los algoritmos de asignación y la planificación de rutas del día siguiente,
en horarios de baja carga, y el aviso a los donantes que llevan más de 20 días sin interacción. Los servicios no
programan nada por su cuenta (no hay `@Scheduled`): los dispara el **cron de la VM** llamando a la API con la clave
de servicio (rol `SISTEMA`), a la 1:00 (matchmaking), a las 3:00 (rutas) y a las 4:00 (inactividad), hora de
Argentina.

- **Un solo lugar para ver y cambiar los horarios:** `/etc/cron.d/donatrack`, generado desde `docker/cron/` del repo.
  Cambiar un horario no requiere recompilar ni redesplegar las imágenes.
- **Una sola ejecución aunque haya varias instancias:** un `@Scheduled` correría una vez por cada contenedor del
  servicio. El cron llama una sola vez y el script evita que dos ejecuciones de la misma tarea se superpongan.
- **Lo mismo que hace el administrador:** el cron usa los mismos endpoints que los botones del panel ("Ejecutar
  matchmaking", "Planificar rutas"), así que no hay dos caminos con lógica distinta.
- **Fallas visibles:** cada ejecución queda en `/var/log/donatrack/cron.log` con su resultado. Si el servicio no
  responde (por ejemplo, porque se está reiniciando) el script reintenta; si la API responde con error, lo registra
  con el mensaje.
- **En una VM aparte, por la red privada:** la VM de cron no tiene IP pública ni Docker. Hace el `curl` al
  balanceador de la VM de servicios (`http://<IP privada>:8080/api`), que le pasa cada llamada a una sola instancia.
  La clave queda en `/etc/donatrack/cron.env`, legible solo por el usuario que corre las tareas. Si se reinicia la VM
  de servicios, el cron no se ve afectado.
- **Hora de Argentina:** la VM se configura en `America/Argentina/Buenos_Aires` para que "la 1 de la mañana" sea la
  de los usuarios y no la de UTC.

El contra es que las tareas dependen de que la VM de cron tenga el cron instalado
(`sudo SERVICIOS_IP=... bash docker/cron/instalar-cron.sh`) y de que la `SERVICE_API_KEY` sea la misma en las dos VMs.
Si todo se levanta en una sola máquina, el mismo script sigue funcionando con `docker exec`.
