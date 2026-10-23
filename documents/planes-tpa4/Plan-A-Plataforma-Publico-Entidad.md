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

### 2.1 Backend: usuarios en `donaciones-service`
Seguir el patrón existente: entidad → `XRepository` + `JpaX extends RepositorioJpa` → `XService` + `XServiceImpl` → controller que implementa `JavalinController`.

- [ ] Entidad `Usuario` (`id`, `username` único, `passwordHash`, `rol`, `perfilId`) + enum `Rol` (`DONANTE`, `ENTIDAD_BENEFICIARIA`, `ADMINISTRADOR`). Registrarla en `META-INF/persistence.xml`.
- [ ] Hash BCrypt (`spring-security-crypto`, ya viene en el BOM de Spring Boot).
- [ ] `POST /api/auth/login` `{username, password}` → `{usuarioId, username, rol, perfilId}` o 401.
- [ ] `POST /api/auth/registro/persona-humana` `{username, password, donante: PersonaHumanaRequest}`.
- [ ] `POST /api/auth/registro/persona-juridica` `{username, password, donante: PersonaJuridicaRequest}`.
- [ ] `POST /api/auth/registro/entidad-beneficiaria` `{username, password, entidad: EntidadBeneficiariaRequest}`.
- [ ] Orden del registro: validar username libre → crear perfil (services existentes) → crear `Usuario` (evita perfiles huérfanos).
- [ ] Admin inicial creado al arrancar desde `ADMIN_USERNAME` / `ADMIN_PASSWORD` (con valores por defecto solo para local).
- [ ] Exponer un método de servicio para crear un usuario para un donante ya existente, que la Persona C usa en la importación CSV.
- [ ] Tests con HSQLDB (como los actuales en `donaciones-service/src/test`).

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
- [ ] Justificación: cliente liviano desacoplado, Handlebars, sesión por cookies (por qué en memoria, defensa contra CSRF con `SameSite`, `HttpOnly`/`Secure`).
- [ ] `Usuario` y `Rol` en `Clases.puml`.

## 6. Cronograma
**[COMPLETAR]** Fechas por bloque. Sugerido: la Tarea 0 en los primeros 1–2 días; después bocetos → landing/login/registro → mapa → pantallas de entidad.

Rama sugerida: `feat/web-base` (Tarea 0) y después `feat/publico-entidad`.
