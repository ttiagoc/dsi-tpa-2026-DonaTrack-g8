# Justificación: autenticación y autorización

## Dos tramos, dos mecanismos
```
Navegador ──cookie de sesión (HttpOnly)──▶ web-service ──Authorization: Bearer <JWT>──▶ donaciones / logística / notificaciones
                               logistica-service ──gRPC, x-api-key en metadata──▶ donaciones-service
                               logistica-service ──REST, X-Api-Key──────────────▶ notificaciones-service
```
- **Navegador ↔ web-service: cookie HTTP**, como pide la consigna. La cookie lleva solo un identificador de
  sesión opaco; `HttpOnly` impide leerla desde JavaScript, `SameSite=Lax` evita que otro sitio la envíe en un POST
  (defensa contra CSRF) y `Secure` la limita a HTTPS en la nube.
- **web-service ↔ APIs: JWT firmado (HS256).** El access token lleva el rol y el perfil del usuario como claims, así
  que cualquier servicio que conozca el secreto sabe quién llama sin consultar la base. Dura 15 minutos; el
  refresh token (7 días) solo sirve para pedir uno nuevo, y al renovarlo se relee el usuario para que una baja o un
  cambio de rol se apliquen. El token queda guardado en la sesión del `web-service` y nunca llega al navegador.
- **Servicio ↔ servicio: clave compartida (`SERVICE_API_KEY`).** Logística no actúa en nombre de un usuario: se
  identifica con la clave y recibe el rol `SISTEMA`, que solo habilita lo que necesita. Hacia donaciones la clave
  viaja en los metadata gRPC (`x-api-key`) y la valida `ApiKeyGrpcServerInterceptor` antes de llegar al código de
  negocio; en las llamadas REST va en el header `X-Api-Key`. El detalle del canal gRPC está en
  [`JustificacionGrpc.md`](JustificacionGrpc.md).

## Roles
| Rol | Quién | Cómo obtiene el usuario |
|---|---|---|
| `DONANTE` | Persona humana o jurídica que dona | Se registra en el sitio, o se le crea al cargarlo el administrador (alta en el depósito o CSV): `AccesoInicialDonante` usa su correo como usuario, genera una contraseña temporal y se la manda por email |
| `ENTIDAD_BENEFICIARIA` | Escuela, comedor, etc. | Se registra en el sitio |
| `CHOFER` | Conductor de un camión | Lo crea el administrador al cargar el camión; su `perfilId` es el id del camión (de logística), así solo ve la hoja de ruta de ese camión y resuelve sus paradas |
| `ADMINISTRADOR` | Personal del depósito | El primero se crea al arrancar con `ADMIN_USERNAME` / `ADMIN_PASSWORD` (`InicializadorAdmin`) |
| `SISTEMA` | Otro servicio | No es un usuario: lo recibe quien presenta la clave compartida |

## Por qué así
- **Javalin y no Spring Security:** los servicios exponen la API con Javalin, que corre en su propio Jetty; el
  filtro de Spring Security no lo intercepta. Se usa lo que ofrece Javalin: roles por ruta
  (`app.get(ruta, handler, Rol.ADMINISTRADOR)`) y un `beforeMatched` que valida el token. Una ruta sin roles es
  pública.
- **Roles + chequeo de dueño:** los roles dicen *qué tipo* de usuario entra; además, un donante solo ve sus
  donaciones y una entidad solo sus necesidades (`Autorizacion.exigirPropietario`). Sin ese chequeo, cualquier donante
  podría leer los datos de otro cambiando un id en la URL.
- **Contraseñas con BCrypt:** se guarda solo el hash, con sal incluida y costo configurable, para que una
  filtración de la base no exponga las contraseñas.
- **401 y no 404 ante credenciales inválidas, con el mismo mensaje** para usuario inexistente y contraseña
  incorrecta: no se revela qué usuarios existen.
- **`Usuario` separado del perfil:** `Donante` y `EntidadBeneficiaria` ya existían y se cargan también por CSV o
  por el administrador; el usuario solo agrega el acceso y referencia al perfil por id (`perfilId`): un donante, una
  entidad o, para el chofer, un camión. Como el camión vive en otro servicio, `perfilId` es un id sin FK.
