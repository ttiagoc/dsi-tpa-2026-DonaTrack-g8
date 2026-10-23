# Justificación: autenticación y autorización

## Dos tramos, dos mecanismos
```
Navegador ──cookie de sesión (HttpOnly)──▶ web-service ──Authorization: Bearer <JWT>──▶ donaciones-service
                                                                logistica-service ──X-Api-Key──▶ donaciones-service
```
- **Navegador ↔ web-service: cookie HTTP**, como pide la consigna. La cookie lleva solo un identificador de
  sesión opaco; `HttpOnly` impide leerla desde JavaScript, `SameSite=Lax` evita que otro sitio la envíe en un POST
  (defensa contra CSRF) y `Secure` la limita a HTTPS en la nube.
- **web-service ↔ APIs: JWT firmado (HS256).** El access token lleva el rol y el perfil del usuario como claims, así
  que cualquier servicio que conozca el secreto sabe quién llama sin consultar la base. Dura 15 minutos; el
  refresh token (7 días) solo sirve para pedir uno nuevo, y al renovarlo se relee el usuario para que una baja o un
  cambio de rol se apliquen. El token queda guardado en la sesión del `web-service` y nunca llega al navegador.
- **Servicio ↔ servicio: clave compartida (`X-Api-Key`).** Logística no actúa en nombre de un usuario: se
  identifica con `SERVICE_API_KEY` y recibe el rol `SISTEMA`, que solo habilita los endpoints que necesita. Cuando
  esas llamadas pasen a gRPC, la misma clave viaja en los metadata.

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
  por el administrador; el usuario solo agrega el acceso y referencia al perfil por id (`perfilId`).
