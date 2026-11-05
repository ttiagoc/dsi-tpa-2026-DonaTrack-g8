# Plan base — Persona B: Donante, Notificaciones y Seguimiento

> Archivo base para que quien tome esta parte arme su plan detallado. Completar las secciones marcadas con **[COMPLETAR]**.

## 1. Alcance

| Bloque | Requisitos de la consigna |
|---|---|
| Persona donante | 5 (filtrar mis donaciones), 6 (catálogo de entidades), 7 (incentivos: misiones e insignias), 8 (notificaciones), 9 (mapa de seguimiento) |
| Entidad beneficiaria (componentes compartidos) | 13 (notificaciones de la entidad), 14 (mapa de seguimiento de la entidad) |
| Documentación | Bocetos de sus pantallas, modelo de gamificación en `Clases.puml`, justificación del diseño de incentivos |

Los requisitos 13 y 14 son de la entidad, pero son **el mismo componente** que el 8 y el 9 con otro filtro. Por eso los hace B: una bandeja de notificaciones y un mapa de seguimiento parametrizados por rol.

## 2. Qué se puede hacer sin esperar la Tarea 0

La Tarea 0 (web-service + login, Persona A) tarda 1–2 días. Mientras tanto, todo el backend de esta parte es independiente:

1. **Dominio de gamificación** (el bloque más grande y el que menos existe hoy).
2. **Endpoint de notificaciones** por destinatario.
3. **Endpoints de seguimiento** en logística.
4. **Filtros** de donaciones.
5. **Bocetos** de las pantallas 5–9, 13 y 14.

## 3. Backend

### 3.1 Gamificación / Incentivos (req. 7) — DESCARTADO
> **Nota:** No estaba contemplado en las consignas previas y su presencia en la consigna 4 fue un error. Se eliminó toda la lógica, modelos, endpoints y vistas de misiones e insignias.

### 3.2 Notificaciones (req. 8 y 13) — `notificaciones-service`
- [ ] Hoy solo existe `POST /api/notificaciones`. La entidad `Notificacion` ya tiene `destinatarioId`, `tipoEvento`, `donacionId`, `mensaje` y `fechaDeEnvio`.
- [ ] Agregar `GET /api/notificaciones?destinatarioId=X&tipoDestinatario=DONANTE|ENTIDAD`, ordenado por fecha descendente.
- [ ] **[COMPLETAR]** Verificar que el `destinatarioId` distinga donante de entidad (si no, agregar un `tipoDestinatario`, porque los ids pueden coincidir).
- [ ] Verificar que se emitan los 4 casos del donante: donación asignada, misión cumplida, subida de categoría y donación recibida por la entidad. Y los 2 de la entidad: asignación y confirmación de recepción.
  - "Recibida por la entidad" = la entidad confirmó la recepción con fotos (req. 12, A): `POST /api/donaciones/{id}/recepcion` → `DonacionServiceImpl.confirmarRecepcion`. Ahí se engancha la notificación (por ejemplo, un evento nuevo en `GestorDeEventos`). Las fotos quedan en `fotosRecepcion` (URLs `/fotos/...` del `web-service`).
- [ ] (Opcional) marcar como leída.
- [ ] **Proteger la API:** hoy `notificaciones-service` no exige autenticación. Ya usa `common-lib`, así que tiene el mismo `AutenticacionHandler` que donaciones: declarar roles en cada ruta (matriz del Plan A, 2.1).
  - `GET /api/notificaciones?...` → `DONANTE` y `ENTIDAD_BENEFICIARIA`, con `Autorizacion.exigirPropietario` (cada uno ve solo las suyas), más `ADMINISTRADOR`.
  - `POST /api/notificaciones` → lo llaman donaciones y logística: `SISTEMA` con `X-Api-Key`, o sin roles si se decide confiar en la red privada (decisión compartida con C, su plan 3.3). Si se protege, los dos servicios tienen que mandar el header.
  - Agregar `JWT_SECRET` (y `SERVICE_API_KEY` si aplica) a notificaciones en `docker-compose.yml` y en la nube: hoy no los tiene.
  - Tests: sin token → 401; el donante pidiendo notificaciones de otro → 403.

### 3.3 Seguimiento (req. 9 y 14) — `logistica-service`
- [ ] Hoy existen `GET /api/camiones/activos` y `GET /api/rutas/{id}/ubicacion`.
- [ ] `Parada` ya tiene `entidadId` y `donacionIds`, así que se puede filtrar:
  - `GET /api/rutas/activas?entidadId=X` → para la entidad.
  - `GET /api/rutas/activas?donacionIds=1,2,3` → para el donante (el web primero pide los ids de sus donaciones a donaciones-service).
- [ ] Respuesta: camión (patente), última ubicación (lat/lng), fecha y hora de la última actualización, paradas.
- [ ] Ojo: el canal donaciones ↔ logística lo pasa a gRPC la Persona C, pero **web → logística sigue siendo HTTP/REST**, así que no hay dependencia.
- [ ] **Roles** (hoy los endpoints de logística son públicos; C protege los de administración): `/api/rutas/activas?entidadId=X` → `ENTIDAD_BENEFICIARIA` (solo su propio `entidadId`, con `exigirPropietario`) y `ADMINISTRADOR`. `?donacionIds=...` → `DONANTE`: logística no sabe de quién es cada donación, así que hay que validar que los ids sean del donante (por ejemplo, que logística acepte el `donanteId` y le pregunte a donaciones, o que el web pase solo los ids que ya obtuvo de `/api/donaciones` del propio donante y logística devuelva datos no sensibles). **[COMPLETAR]** elegir una.

### 3.4 Filtros de donaciones (req. 5) — `donaciones-service`
- [ ] Hoy `GET /api/donaciones` devuelve todas y `GET /api/donaciones/estado/{estado}` filtra solo por estado.
- [ ] Agregar `GET /api/donaciones?donanteId=X&estado=Y&categoriaId=Z&subcategoriaIds=...`.
  - **Ojo, ya lo tocó A (req. 11):** `GET /api/donaciones` acepta `?entidadId=X` (`DonacionController`): la entidad solo pide las suyas (`exigirPropietario`) y sin filtro solo entran admin y servicios. Sumar `donanteId` igual (`exigirPropietario(ctx, DONANTE, id)`) y combinar los filtros en vez de reemplazar ese bloque. `DonacionResponse` ya trae `subcategoria`, `categoria`, `fechaAsignacion`, `fechaEstado`, `necesidad` y `fotosRecepcion`.
- [ ] Reutilizar `GET /api/categorias` y `GET /api/categorias/{id}/subcategorias` para armar los selects.

### 3.5 Catálogo de entidades (req. 6)
- [ ] Ya existe `GET /api/entidad-beneficiaria` (incluye necesidades). Probablemente no haga falta backend nuevo.

## 4. Pantallas (`web-service`, después de pusheada la Tarea 0 a `entrega-4`)

| Req | Ruta sugerida | Plantilla |
|---|---|---|
| 5 | `/donante/donaciones` | `templates/donante/mis-donaciones.hbs` (tabla + filtros en formulario GET) |
| 6 | `/donante/entidades` | `templates/donante/entidades.hbs` (tarjetas con necesidades) |
| 7 | ~~/donante/incentivos~~ | *Descartado (misiones e insignias fuera de alcance)* |
| 8 / 13 | `/donante/notificaciones`, `/entidad/notificaciones` | `templates/partials/bandeja-notificaciones.hbs` compartido |
| 9 / 14 | `/donante/seguimiento`, `/entidad/seguimiento` | `templates/partials/mapa-seguimiento.hbs` compartido (Leaflet) |

- Clientes a crear en el web: `NotificacionesApiClient`, `LogisticaApiClient` (y métodos nuevos en `DonacionesApiClient`).
- Usar `usuario.perfilId` de la sesión como `donanteId` / `entidadId`. **Nunca** tomar el id desde la URL o un formulario (un donante no tiene que poder ver los datos de otro).
- **[COMPLETAR]** Mapa: ¿se refresca solo (JS con `fetch` cada N segundos a un endpoint JSON del web) o recargando la página? Si es con JS, el endpoint JSON también tiene que pasar por el middleware de sesión.

## 5. Dependencias con el resto
- **Recibe de A:** la Tarea 0 (layout, navbar, sesión, middleware) y la convención de plantillas. Para la parte de pantallas, tomarla de `entrega-4` una vez pusheada.
- **Para reutilizar de A** (ya en `entrega-4`): `SituacionDonacion` (el estado de una donación en palabras y la clase de su etiqueta), las etiquetas `etiqueta--info`, `etiqueta--error` y `etiqueta--neutra`, y las reglas de archivos de `formularios.js` (`type="file"`, `data-max-archivos`, `data-max-mb`). El mapa de entregas (`/mapa`, req. 2) es una página común a todos los roles: su link está en la sección común "Plataforma" del menú lateral, no en el bloque de ningún rol.
- **Comparte con A:** el layout con sesión cambió a **menú lateral + barra superior** (ver "Convenciones de pantallas" en [`AGENTS.md`](../../AGENTS.md#convenciones-de-pantallas) y el boceto `10-necesidades.png`). A es dueño del layout, de la barra y del bloque de la entidad en el menú. B arma el bloque del donante y agrega en el bloque de la entidad los links a sus pantallas 13 y 14 (notificaciones y seguimiento), siguiendo el formato que explica `partials/menu-lateral.html.hbs`.
- **Cambios de A después de la revisión de diseño:** la barra superior ya no tiene título (cada página lo pone en su `<h1>`), el botón para achicar el menú está en su cabecera, el footer aparece también con sesión y **todo modal lleva una cruz** (`.modal__cerrar` con `data-cierra`) y `autofocus` en la acción segura (ver `AGENTS.md` y `entidad/necesidades.html.hbs`).
- **Unidad de medida (afecta el alta de donaciones, req. 5):** cada necesidad se mide en `UNIDADES`, `KILOGRAMOS` o `LITROS` (`UnidadDeMedida`). Los bienes donados se siguen contando en unidades, y para las necesidades en kg o litros se usan `pesoKgPorUnidad` y `volumenM3PorUnidad` del bien: un bien sin esos datos no suma. Conviene que el formulario de donación pida el peso por unidad (y el volumen en los líquidos). El matchmaking no cambia.
- **Comparte con C:** si C cambia los DTOs de donaciones al pasar a gRPC, avisar; los endpoints REST que usa el web no deberían cambiar.
- **Archivos compartidos:** `persistence.xml` (donaciones), `Clases.puml`, `navbar.hbs`, `styles.css`. Regla: agregar al final, en una sección propia.

## 6. Entregables de documentación
- [ ] Bocetos de las pantallas 5–9, 13 y 14 en `documents/bocetos/`, **antes** de implementarlas. Seguir el flujo de [`AGENTS.md`](../../AGENTS.md#flujo-para-cada-pantalla-nueva): boceto (en lo posible en Figma), PNG con formato `punto-nombre.png` y después el código.
- [ ] Clases de gamificación en `Clases.puml` (paquete propio).
- [ ] Justificación del modelo de incentivos (por qué estrategia o evento, cómo se extiende con nuevas misiones).

## 7. Cronograma
**[COMPLETAR]** Fechas por bloque. Sugerido: días 1–2 gamificación + bocetos; días 3–4 endpoints de notificaciones, seguimiento y filtros; después las pantallas.

Rama: todo en `entrega-4` hasta que esté terminado el trabajo de todo el equipo.
