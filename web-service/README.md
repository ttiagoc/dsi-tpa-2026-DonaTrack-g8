# web-service — sitio web de DonaTrack

Cliente liviano con MVC del lado del servidor: **Javalin** atiende HTTP, **Handlebars** arma el HTML y los
datos se piden por HTTP a las APIs (`donaciones-service`, `logistica-service`, `notificaciones-service`).
No tiene base de datos ni depende de `common-lib`. Sigue el apunte de la cátedra *"Introducción a MVC Web del
lado del servidor con Javalin"*; las decisiones están en el Plan A (sección 2.2,
[`documents/planes-tpa4/Plan-A-Plataforma-Publico-Entidad.md`](../documents/planes-tpa4/Plan-A-Plataforma-Publico-Entidad.md))
y en [`entregables/JustificacionWebService.md`](../entregables/JustificacionWebService.md).

## Levantarlo en local

```bash
# 1. Base + APIs (desde la raíz). Alternativa: .\run.ps1 start levanta todo, incluida la web
docker compose up -d
mvn spring-boot:run -pl donaciones-service
# 2. La web, en otra terminal
mvn spring-boot:run -pl web-service
```

Abrir <http://localhost:7070> y entrar con el admin inicial (`ADMIN_USERNAME` / `ADMIN_PASSWORD`, por defecto
`admin` / `admin1234`), o registrarse. Con Docker: `docker compose --profile full up -d --build` levanta todo.

| Variable | Por defecto | Para qué |
|---|---|---|
| `PORT` | `7070` | Puerto del sitio |
| `DONACIONES_API_URL` | `http://localhost:8080/api` | API de donaciones (incluye `/api/auth`) |
| `LOGISTICA_API_URL` | `http://localhost:8081/api` | API de logística |
| `NOTIFICACIONES_API_URL` | `http://localhost:8082/api` | API de notificaciones |
| `COOKIE_SECURE` | `false` | `true` en la nube: la cookie de sesión solo viaja por HTTPS |
| `SESION_MINUTOS_INACTIVIDAD` | `120` | Vencimiento de la sesión por inactividad |

## Estructura

```
src/main/java/ar/edu/utn/frba/ddsi/web/
├── server/Server.java        configuración de Javalin: plantillas, estáticos, sesión, páginas de error
├── server/Router.java        TODAS las rutas, con sus roles
├── server/templates/         JavalinRenderer + JavalinHandlebars (de la cátedra)
├── controllers/              un método (Context ctx) por ruta; Modelo y Formulario son helpers
├── middlewares/              AutorizacionMiddleware: valida los roles que declara cada ruta
├── sesion/                   SesionUsuario (tokens + usuario), Alertas (mensajes flash), RolUsuario
├── clients/                  ApiClient (base) y un XApiClient por API
├── dto/                      records espejo de los JSON de las APIs
└── exceptions/               se traducen a redirect o a página de error en Server
src/main/resources/
├── templates/                vistas .html.hbs (layouts/, partials/, una carpeta por rol)
└── public/                   css/styles.css, img/ (se sirven en /css/..., /img/...)
```

## Cómo agregar una pantalla

1. **Controller:** un método `public void x(Context ctx)` en el controller de tu área (o uno nuevo, anotado con
   `@Component` y agregado al constructor de `Router`).
2. **Ruta:** en `Router`, dentro de tu bloque (`// --- Donante (B) ---`, `// --- Administrador (C) ---`),
   **con sus roles**: `app.get("/donante/donaciones", donaciones::listar, DONANTE);`. Una ruta sin roles es pública.
3. **Vista:** `templates/<rol>/x.html.hbs` con el layout:
   ```handlebars
   {{#partial "titulo"}}Mis donaciones · DonaTrack{{/partial}}
   {{#partial "contenido"}}
     <h1>Mis donaciones</h1>
   {{/partial}}
   {{> layouts/base}}
   ```
4. **Link:** en `partials/navbar.html.hbs`, dentro del `{{#if usuario.esDonante}}` (o el de tu rol).

```java
public void listar(Context ctx) {
    Map<String, Object> modelo = Modelo.base(ctx);           // siempre arrancar de acá
    modelo.put("donaciones", donacionesApi.misDonaciones(ctx));
    ctx.render("donante/donaciones.html.hbs", modelo);
}
```

### Lo que siempre está en el modelo (`Modelo.base(ctx)`)

| Variable | Contenido |
|---|---|
| `usuario` | `null` si es un visitante. Si no: `username`, `rol`, `perfilId` (id del donante o de la entidad), `usuarioId`, `panel`, `esDonante`, `esEntidad`, `esAdmin` |
| `alertas` | mensajes pendientes (los muestra el layout) |

`perfilId` es el id que hay que mandar a la API para "lo mío" (`/donantes/{perfilId}`, `/entidad-beneficiaria/{perfilId}`).

## Formularios

- Patrón **POST → redirect → GET**: si sale bien, `Alertas.exito(ctx, "Se guardó la donación")` y `ctx.redirect(...)`.
  La alerta aparece una sola vez en la página siguiente.
- Si hay errores, se vuelve a renderizar con `status(400)` usando `Formulario`:
  ```java
  Formulario form = new Formulario(ctx);
  String nombre = form.requerido("nombre", "El nombre");      // junta "El nombre es obligatorio"
  if (form.tieneErrores()) {
      ctx.status(400).render(VISTA, form.enModelo(Modelo.base(ctx), "Revisá los campos marcados."));
      return;
  }
  ```
  En la vista: `{{> partials/campo nombre="nombre" etiqueta="Nombre" valor=form.nombre errorCampo=errores.nombre requerido=true}}`.
- Los mensajes de error del backend llegan como `ApiException` con el texto listo para mostrar
  (`e.getMessage()`); `e.esErrorDelUsuario()` distingue un 4xx (mostrarlo en el form) de un 5xx (dejarlo subir).
- Los formularios HTML solo mandan **GET y POST**: borrar o editar es `POST /x/{id}/eliminar`, `POST /x/{id}/editar`.
  Nada que modifique datos va por GET.

## Llamar a una API

Agregar un método al `XApiClient` que corresponda (o crear uno nuevo extendiendo `ApiClient` y registrarlo como
`@Bean` en `config/ClientesConfig`):

```java
public List<DonacionResponse> misDonaciones(Context ctx) {
    return get(ctx, "/donaciones?donanteId=" + perfilId(ctx), new TypeReference<>() {});
}
```

- Los métodos con `Context` (`get`, `post`, `put`, `delete`) mandan el token de la sesión. Si venció, `ApiClient`
  lo renueva y reintenta solo; si ya no se puede, el usuario vuelve al login.
- `getPublico` / `postPublico` no mandan token (login, registro, categorías).
- Errores: 403 → página 403; 404 → página 404; API caída → página 503; el resto → página 500.
  Para mostrar un 404 propio desde un controller: `throw new NoEncontradoException("No existe la donación 3")`.

## CSS

`public/css/styles.css` tiene las variables (`--color-primario`, `--espacio-4`, ...) y estas clases:
`boton` (`--secundario`, `--peligro`, `--chico`, `--ancho`), `tarjeta` (`--opcion`, `--vacia`), `grilla grilla--2|3`,
`formulario`, `campo`, `alerta alerta--exito|error|info`, `tabla` (dentro de `tabla-contenedor`), `etiqueta`,
`acciones`, `texto-suave`, `seccion`. Lo propio de cada área va al final del archivo, en su sección.

## Sesión y seguridad

- La sesión vive en memoria del servidor (Jetty); el navegador solo guarda la cookie `donatrack_session` con un id
  aleatorio. Al loguearse se cambia el id. Logout = `POST /logout`.
- La cookie es `HttpOnly` (el JavaScript no la lee), `SameSite=Lax` (no viaja en POSTs desde otros sitios: defensa
  contra CSRF) y `Secure` con `COOKIE_SECURE=true`.
- Los roles de `Router` solo deciden qué páginas se muestran: la API vuelve a validar cada llamada con el JWT.
- Handlebars escapa todo lo que se imprime con `{{ }}`. No usar `{{{ }}}` con datos que cargó un usuario.

## Tests

`WebFlujoTest` levanta la web contra una API falsa y recorre las pantallas como un navegador (cookies, redirects,
formularios): `mvn -pl web-service -am test`.
