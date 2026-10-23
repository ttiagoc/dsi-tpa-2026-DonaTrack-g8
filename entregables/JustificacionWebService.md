# Justificación de diseño — Sitio web (`web-service`)

Complementa a [`JustificacionAutenticacion.md`](JustificacionAutenticacion.md), que cubre JWT, BCrypt y los roles de las APIs.

## 1. MVC del lado del servidor con un cliente liviano separado

El sitio es un módulo aparte (`web-service`) que arma el HTML en el servidor y obtiene los datos **solo por HTTP** de las APIs
(`donaciones-service`, `logistica-service`, `notificaciones-service`). No tiene base de datos ni depende de `common-lib`.

- **Desacoplamiento:** las APIs siguen siendo la única puerta al dominio. El sitio es un cliente más, igual que Postman o
  logística; si mañana se agrega una app móvil, usa la misma API y las mismas reglas de seguridad.
- **Despliegue independiente:** el sitio y las APIs escalan y se despliegan por separado.
- **Seguridad en profundidad:** el sitio decide qué páginas mostrar según el rol, pero cada llamada vuelve a validarse en la API
  con el token. Un error en la web no expone datos ajenos.

Siguiendo el apunte de la cátedra, el código se organiza en:
- **Modelo:** los datos que devuelven las APIs (records en `dto/`) y el mapa que cada controller arma para su vista.
- **Vista:** plantillas Handlebars en `templates/`, una carpeta por rol, con un layout y partials comunes.
- **Controlador:** clases planas en `controllers/`, un método `(Context ctx)` por ruta, sin heredar ni implementar nada.
- Las rutas se declaran todas en `Router` con referencias a métodos (`app.get("/login", sesion::mostrarLogin)`), para que el
  `main` no "crezca sin control".

## 2. Javalin + Handlebars

- **Javalin** es un framework liviano: resuelve lo mínimo del pedido HTTP (rutas, parámetros, sesión, respuesta) y ya lo usan
  las APIs del proyecto, así que el equipo no aprende una tecnología nueva. Lo que no trae (plantillas, cliente HTTP) se agrega
  con librerías puntuales.
- **Handlebars** permite que las vistas sean **reutilizables** (un layout, partials para navbar, alertas y campos de formulario)
  y **declarativas** (el HTML se lee como HTML, con `{{usuario.username}}` donde va el dato), en lugar de concatenar strings.
  Además escapa por defecto todo lo que imprime con `{{ }}`, lo que evita inyectar HTML o scripts con datos que cargó un usuario.
- Se usan el `JavalinRenderer` y el `JavalinHandlebars` del ejemplo de la cátedra, con dos agregados: leer `records` de Java
  y los helpers de comparación (`eq`) para los formularios.

## 3. Sesión con cookies

Al loguearse, la API devuelve un access token (15 min) y un refresh token (7 días). El sitio **no** se los da al navegador:

- Los guarda en la **sesión del servidor** (memoria de Jetty). El navegador solo recibe la cookie `donatrack_session` con un
  id aleatorio, que no dice nada por sí mismo.
- Antes de cada llamada a la API agrega `Authorization: Bearer <token>`. Si la API responde 401 porque el token venció, pide
  uno nuevo con el refresh token y reintenta una vez. Si eso también falla, cierra la sesión y vuelve al login.

Atributos de la cookie:

| Atributo | Para qué |
|---|---|
| `HttpOnly` | El JavaScript de la página no puede leerla: un script inyectado no puede robar la sesión. |
| `SameSite=Lax` | El navegador no la manda en pedidos POST que vienen de otros sitios: defensa contra CSRF. Por eso toda acción que modifica datos (incluido el logout) es POST, nunca GET. |
| `Secure` (en la nube) | Solo viaja por HTTPS. Se activa con `COOKIE_SECURE=true`; en local se usa http. |
| Vencimiento por inactividad | 2 horas sin uso y la sesión se descarta. |

Además, **al loguearse se cambia el id de la sesión**, para que un id que alguien haya fijado antes (fijación de sesión) no
quede asociado al usuario autenticado.

**Por qué en memoria:** es lo más simple y alcanza para una sola instancia del sitio. La contra es que, si el servidor se
reinicia, los usuarios tienen que volver a loguearse, y que con varias instancias habría que compartir las sesiones (por
ejemplo en una base o en Redis). Para el alcance del TP, una instancia es suficiente.

## 4. Formularios y errores

- Patrón **POST → redirect → GET**: si un formulario sale bien se redirige, para que recargar la página no lo reenvíe. El
  mensaje de éxito viaja en la sesión como alerta "flash" y se muestra una sola vez.
- Validación en dos capas: el sitio controla lo que no necesita al dominio (obligatorios, que las contraseñas coincidan) y las
  reglas de negocio las valida la API; su mensaje se muestra arriba del formulario, que vuelve con los datos cargados (nunca la
  contraseña).
- Las páginas de error (403, 404, 500 y 503 si una API no responde) usan el mismo layout que el resto del sitio.
