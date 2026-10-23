# Plan base — Persona A: Plataforma web, Público y Entidad Beneficiaria

> Archivo base para que quien tome esta parte arme su plan detallado. Completar las secciones marcadas con **[COMPLETAR]**.

## 1. Alcance

| Bloque | Requisitos de la consigna |
|---|---|
| **Tarea 0 (bloqueante para todos)** | Módulo `web-service`, layout/CSS base, `Usuario` + login/registro/logout con cookies, middleware de roles |
| Público | 1 (landing), 2 (mapa de donaciones entregadas), 3 (registro), 4 (login) |
| Entidad beneficiaria | 10 (registrar necesidades), 11 (donaciones asignadas), 12 (confirmar recepción con fotos) |
| Documentación | Bocetos de sus pantallas, justificación de cliente liviano + Handlebars + sesión por cookies, `Usuario` en `Clases.puml` |

**No incluye:** los requisitos 13 y 14 (notificaciones y mapa de seguimiento de la entidad), que hace la Persona B porque reutilizan el mismo componente que el donante.

## 2. Tarea 0 — base del `web-service` (prioridad máxima)

Mergear a `main` lo antes posible: B y C la necesitan para crear sus pantallas.

### 2.1 Backend: autenticación en `donaciones-service` — ✅ hecho (rama `entrega-4`)
Sigue el ejemplo de clase `gestion-alumnos-server` (JWT + BCrypt + roles), adaptado a Javalin: Spring Security no intercepta el Jetty de Javalin, así que se usan roles por ruta (`app.get(ruta, handler, Rol.X)`) y un `beforeMatched`. Justificación completa en [`entregables/JustificacionAutenticacion.md`](../../entregables/JustificacionAutenticacion.md).

- [x] `Usuario` (JPA, `username` único, `passwordHash` BCrypt, `rol`, `perfilId`, `activo`) + `UsuarioRepository`/`JpaUsuario`.
- [x] En `common-lib/security`: `Rol` (`DONANTE`, `ENTIDAD_BENEFICIARIA`, `ADMINISTRADOR`, `SISTEMA`), `JwtUtil` (access 15 min, refresh 7 días), `AutenticacionHandler` (401/403) y `Autorizacion.exigirPropietario` (un donante solo ve lo suyo; ADMINISTRADOR y SISTEMA pasan siempre).
- [x] `AuthService`/`AuthServiceImpl`: login, refresh, registro de los 3 tipos (perfil primero; si falla el usuario se borra el perfil), `crearUsuarioParaDonante` para el CSV de la Persona C, admin inicial con `ADMIN_USERNAME` / `ADMIN_PASSWORD`.
- [x] Todos los endpoints de donaciones protegidos según la matriz de abajo.
- [x] Logística manda `X-Api-Key` (`SERVICE_API_KEY`) en sus llamadas a donaciones → rol `SISTEMA`. Coordinado con la Persona C (su plan, sección 3.3).
- [x] Tests: `JwtUtilTest`, `UsuarioRepositoryJpaTest`, `AuthServiceTest`, `AuthApiTest`, `RestTemplateConfigTest`; `DonacionesApiPersistenciaTest` y el E2E entran como admin.
- [x] Postman (carpeta "00 - Autenticación", el login guarda `{{access_token}}`), `demo_presentacion.sh`, `docker-compose.yml` y `Clases.puml` actualizados.
- [ ] **Falta:** probar el flujo de rutas con los servicios levantados (`docker compose --profile full up -d --build` + `demo_presentacion.sh`).

#### Contrato de `/api/auth` (lo consume el `web-service`)
| Método y ruta | Body | Respuesta |
|---|---|---|
| `POST /api/auth/login` | `{username, password}` | `200 {accessToken, refreshToken, usuario: {usuarioId, username, rol, perfilId}}` · `401` |
| `POST /api/auth/refresh` | `{refreshToken}` | `200 {accessToken, refreshToken}` · `401` |
| `POST /api/auth/registro/persona-humana` | `{username, password, donante: PersonaHumanaRequest}` | `201` igual que el login (queda logueado) · `400` |
| `POST /api/auth/registro/persona-juridica` | `{username, password, donante: PersonaJuridicaRequest}` | ídem |
| `POST /api/auth/registro/entidad-beneficiaria` | `{username, password, entidad: EntidadBeneficiariaRequest}` | ídem |
| `GET /api/auth/me` | — (`Authorization: Bearer`) | `200 usuario` |

El `web-service` manda `Authorization: Bearer <accessToken>` en cada llamada; si recibe 401, pide `/api/auth/refresh` y reintenta una vez. Errores: sin token o token inválido → `401`; rol equivocado o recurso ajeno → `403`.

#### Matriz de roles (`propietario` = además `Autorizacion.exigirPropietario`)
| Endpoint | Público | DONANTE | ENTIDAD | ADMIN | SISTEMA |
|---|---|---|---|---|---|
| `/api/auth/*` (salvo `me`), `GET /api/categorias`, `.../subcategorias` | ✓ | | | | |
| `POST /api/categorias`, `.../subcategorias`, `/api/matchmaking/*` | | | | ✓ | |
| `POST /api/donaciones` | | propietario | | ✓ | |
| `GET /api/donaciones` | | | | ✓ | ✓ |
| `GET /api/donaciones/{id}` | | propietario | propietario | ✓ | ✓ |
| `DELETE /api/donaciones/{id}` | | | | ✓ | |
| `PUT /api/donaciones/{id}/estado`, `GET /api/donaciones/estado/{estado}` | | | | ✓ | ✓ |
| `POST /api/donaciones/{id}/fotos` | | | propietario | ✓ | |
| `POST /api/donaciones/recepciones` | | | propietario | ✓ | ✓ |
| `GET/PUT /api/donantes/{id}`, `PUT .../persona-*/{id}` | | propietario | | ✓ | |
| `GET /api/donantes/{id}/contacto` | | propietario | | ✓ | ✓ |
| `GET /api/donantes`, `DELETE`, `POST /api/donantes/persona-*`, inactividad | | | | ✓ | |
| `GET/PUT /api/entidad-beneficiaria/{id}`, necesidades | | | propietario | ✓ | |
| `GET /api/entidad-beneficiaria`, `POST`, `DELETE` | | | | ✓ | |
| `GET /api/entidad-beneficiaria/{id}/contactos` | | | | ✓ | ✓ |

**Regla para B y C:** todo endpoint nuevo declara sus roles al crearlo (sin roles = público) y, si es "solo lo mío", llama a `exigirPropietario`.

#### Pendientes y riesgos
- `POST /api/donaciones/recepciones` lo usan logística (entrega del camión) y la entidad (req. 12): separar la confirmación de la entidad en `POST /api/donaciones/{id}/recepcion` al hacer el req. 12.
- Cuando la Persona C pase logística → donaciones a gRPC, el canal gRPC se protege con la misma `SERVICE_API_KEY` (interceptores) y los endpoints REST que solo usaba logística pierden el rol `SISTEMA`.
- `notificaciones-service` sigue sin proteger (decisión de C: red privada o la misma clave).
- Logout: con JWT el logout es borrar la sesión del `web-service`; el refresh token sigue válido hasta que vence (alcanza para el TP).
- En la nube, `JWT_SECRET` (≥ 32 bytes, igual en todos los servicios), `SERVICE_API_KEY` y `ADMIN_PASSWORD` tienen que tener valores propios.

### 2.2 Módulo `web-service`
- [ ] Agregar `<module>web-service</module>` al `pom.xml` raíz.
- [ ] Dependencias: Spring Boot starter (DI/config, igual que los otros servicios), Javalin 6.1.3, `com.github.jknack:handlebars:4.3.1`, `spring-web` (`RestTemplate`). **No depende de `common-lib`**: el cliente liviano solo habla HTTP con los servicios.
- [ ] `application.properties`: `server.port=${PORT:7070}`, `DONACIONES_API_URL`, `LOGISTICA_API_URL`, `NOTIFICACIONES_API_URL`.
- [ ] Servidor Javalin: renderer Handlebars (`FileRenderer` propio con resolvers de Map/JavaBean/Method para soportar records), estáticos en `/public`, páginas de error 404/500 en HTML.
- [ ] Paquetes: `controllers/`, `clients/` (`DonacionesApiClient`; los errores del backend se transforman en mensajes para los formularios), `sesion/`, `middlewares/`, `dto/`.
- [ ] Plantillas: `layouts/base.hbs` (patrón `{{#partial "contenido"}}` + `{{> layouts/base}}`), `partials/navbar.hbs` (según rol), `partials/alertas.hbs`, `partials/footer.hbs`.
- [ ] `public/css/styles.css`: variables (colores, espaciado, tipografía), botones, formularios, tablas, tarjetas, alertas, grilla responsive.

### 2.3 Sesión con cookies HTTP
- [ ] Cookie `donatrack_session` con `HttpOnly`, `SameSite=Lax` y `Secure` en la nube. Guarda solo un token aleatorio; los datos de la sesión quedan en el servidor (`GestorDeSesiones`).
- [ ] `GET/POST /login` → redirige al panel según el rol. `POST /logout` borra la cookie.
- [ ] `GET /registro` (selector) → formularios de persona humana, persona jurídica y entidad.
- [ ] Middleware: un `before` global carga el usuario para el navbar; un `before` por prefijo (`/donante/*`, `/entidad/*`, `/admin/*`): sin sesión redirige a `/login`, con rol equivocado devuelve 403.
- [ ] Paneles vacíos `/donante`, `/entidad` y `/admin` como punto de entrada para B y C.

### 2.4 Contrato para el equipo (`web-service/README.md`)
- [ ] Cómo agregar una pantalla (controller + `templates/<rol>/x.hbs` + link en el navbar).
- [ ] Variables disponibles siempre en el modelo: `usuario.username`, `usuario.rol`, `usuario.perfilId`.
- [ ] Convenciones de clases CSS, alertas y errores de formulario.
- [ ] Cómo crear un `XApiClient` para un backend nuevo.
- [ ] Cómo levantar todo en local (3 servicios + web).

### 2.5 Listo cuando
- [ ] `mvn package` compila todo el reactor.
- [ ] Registro + login funcionan para los 3 roles y cada uno cae en su panel.
- [ ] El rol equivocado da 403; sin sesión, redirige a `/login`; el logout borra la cookie.
- [ ] PR mergeado a `main` y aviso a B y C.

## 3. Pantallas propias (después de la Tarea 0)

| Req | Pantalla | Backend existente | Backend a agregar |
|---|---|---|---|
| 1 | Landing `/` + `/legal` | — | — |
| 2 | Mapa público `/mapa` (Leaflet + OSM, popup con detalle y fotos) | `GET /api/donaciones`, `GET /api/entidad-beneficiaria` | `latitud`/`longitud` en `EntidadBeneficiaria` (hoy no existen); `GET /api/donaciones/entregadas` con entidad, ubicación, bienes y fotos |
| 3 | Registro | (Tarea 0) | — |
| 4 | Login | (Tarea 0) | — |
| 10 | `/entidad/necesidades` (alta + listado, recurrente vs extraordinaria) | `GET/POST/DELETE /api/entidad-beneficiaria/{id}/necesidades` | — |
| 11 | `/entidad/donaciones` (estado de las asignadas) | `GET /api/donaciones` (sin filtros) | `GET /api/donaciones?entidadId=X` (`Donacion` ya tiene `entidadBeneficiariaAsignada`). Coordinar con B, que agrega filtros al mismo endpoint |
| 12 | `/entidad/donaciones/{id}/recepcion` (confirmar + fotos) | `POST /api/donaciones/{id}/fotos` (recibe URLs), `POST /api/donaciones/recepciones` | Subir el archivo a un storage externo y mandar las URLs |

### Decisiones a tomar
- **[COMPLETAR]** Coordenadas de las entidades: ¿se cargan en el registro o se geocodifica la dirección (Nominatim)?
- **[COMPLETAR]** Storage de fotos: Supabase Storage o Cloudinary (el disco del PaaS no persiste). Coordinarlo con la Persona C por las variables de entorno.
- **[COMPLETAR]** Ajustes según el documento de MVC de la cátedra (estructura de paquetes, motor de plantillas).

## 4. Dependencias con el resto
- **Entrega a B y C:** la Tarea 0 y la API de usuarios (C la usa en el CSV).
- **Recibe de C:** las URLs y variables de entorno del despliegue.
- **Archivos compartidos** (riesgo de conflictos): `styles.css`, `navbar.hbs`, `persistence.xml`, `pom.xml` raíz, `Clases.puml`. Regla: A es dueño del layout y del navbar; los demás solo agregan sus links o su sección.

## 5. Entregables de documentación
- [ ] Bocetos de las pantallas 1–4 y 10–12 en `documents/bocetos/`, **antes** de implementarlas.
- [ ] Justificación: cliente liviano desacoplado, Handlebars, sesión por cookies (por qué en memoria, defensa contra CSRF con `SameSite`, `HttpOnly`/`Secure`). La parte de JWT, BCrypt y roles ya está en `entregables/JustificacionAutenticacion.md`; falta sumar la del `web-service`.
- [x] `Usuario` y `Rol` en `Clases.puml`.

## 6. Cronograma
**[COMPLETAR]** Fechas por bloque. Sugerido: la Tarea 0 en los primeros 1–2 días; después bocetos → landing/login/registro → mapa → pantallas de entidad.

Rama: `entrega-4` (autenticación del backend, ya hecha; el `web-service` sigue en la misma rama o en `feat/web-base`) y después `feat/publico-entidad`.
