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

### 3.1 Gamificación (req. 7 y parte del 8) — `donaciones-service`
Hoy **no existe nada** de misiones, insignias ni categorías de donante en el código. Hay que modelarlo antes de hacer la pantalla.

- [ ] **[COMPLETAR]** Releer la consigna de entregas anteriores para ver si definen misiones, insignias y categorías concretas. Si no, proponer un modelo y validarlo con el ayudante.
- [ ] Modelo sugerido para discutir:
  - `Mision`: descripción y condición de cumplimiento (por ejemplo, N donaciones o donar en cierta categoría). Conviene una estrategia o interfaz `CondicionMision` para que sea extensible.
  - `Insignia`: se otorga al cumplir una misión.
  - `CategoriaDonante` (o "nivel"): sube según puntos o donaciones; se notifica cuando sube.
  - Progreso del donante por misión (para la barra de progreso de la pantalla).
- [ ] Solo aplica a **persona humana** (la consigna lo aclara en el req. 7).
- [ ] ¿Cuándo se evalúa? Recomendado: reaccionar a eventos que ya existen (`GestorDeEventos`, `EventManagerDonaciones`, por ejemplo al registrar o entregar una donación), igual que los listeners actuales.
- [ ] Al cumplir una misión o subir de categoría → enviar notificación (mismo mecanismo que `DonacionAsignadaListener`).
- [ ] `GET /api/donantes/{id}/incentivos` → misiones con su progreso, insignias obtenidas y categoría actual.
- [ ] Entidades en `persistence.xml` + tests de dominio.

### 3.2 Notificaciones (req. 8 y 13) — `notificaciones-service`
- [ ] Hoy solo existe `POST /api/notificaciones`. La entidad `Notificacion` ya tiene `destinatarioId`, `tipoEvento`, `donacionId`, `mensaje` y `fechaDeEnvio`.
- [ ] Agregar `GET /api/notificaciones?destinatarioId=X&tipoDestinatario=DONANTE|ENTIDAD`, ordenado por fecha descendente.
- [ ] **[COMPLETAR]** Verificar que el `destinatarioId` distinga donante de entidad (si no, agregar un `tipoDestinatario`, porque los ids pueden coincidir).
- [ ] Verificar que se emitan los 4 casos del donante: donación asignada, misión cumplida, subida de categoría y donación recibida por la entidad. Y los 2 de la entidad: asignación y confirmación de recepción.
- [ ] (Opcional) marcar como leída.

### 3.3 Seguimiento (req. 9 y 14) — `logistica-service`
- [ ] Hoy existen `GET /api/camiones/activos` y `GET /api/rutas/{id}/ubicacion`.
- [ ] `Parada` ya tiene `entidadId` y `donacionIds`, así que se puede filtrar:
  - `GET /api/rutas/activas?entidadId=X` → para la entidad.
  - `GET /api/rutas/activas?donacionIds=1,2,3` → para el donante (el web primero pide los ids de sus donaciones a donaciones-service).
- [ ] Respuesta: camión (patente), última ubicación (lat/lng), fecha y hora de la última actualización, paradas.
- [ ] Ojo: el canal donaciones ↔ logística lo pasa a gRPC la Persona C, pero **web → logística sigue siendo HTTP/REST**, así que no hay dependencia.

### 3.4 Filtros de donaciones (req. 5) — `donaciones-service`
- [ ] Hoy `GET /api/donaciones` devuelve todas y `GET /api/donaciones/estado/{estado}` filtra solo por estado.
- [ ] Agregar `GET /api/donaciones?donanteId=X&estado=Y&categoriaId=Z&subcategoriaIds=...`.
- [ ] Reutilizar `GET /api/categorias` y `GET /api/categorias/{id}/subcategorias` para armar los selects.

### 3.5 Catálogo de entidades (req. 6)
- [ ] Ya existe `GET /api/entidad-beneficiaria` (incluye necesidades). Probablemente no haga falta backend nuevo.

## 4. Pantallas (`web-service`, después de pusheada la Tarea 0 a `entrega-4`)

| Req | Ruta sugerida | Plantilla |
|---|---|---|
| 5 | `/donante/donaciones` | `templates/donante/mis-donaciones.hbs` (tabla + filtros en formulario GET) |
| 6 | `/donante/entidades` | `templates/donante/entidades.hbs` (tarjetas con necesidades) |
| 7 | `/donante/incentivos` | `templates/donante/incentivos.hbs` (misiones con progreso, insignias, categoría). Solo persona humana |
| 8 / 13 | `/donante/notificaciones`, `/entidad/notificaciones` | `templates/partials/bandeja-notificaciones.hbs` compartido |
| 9 / 14 | `/donante/seguimiento`, `/entidad/seguimiento` | `templates/partials/mapa-seguimiento.hbs` compartido (Leaflet) |

- Clientes a crear en el web: `NotificacionesApiClient`, `LogisticaApiClient` (y métodos nuevos en `DonacionesApiClient`).
- Usar `usuario.perfilId` de la sesión como `donanteId` / `entidadId`. **Nunca** tomar el id desde la URL o un formulario (un donante no tiene que poder ver los datos de otro).
- **[COMPLETAR]** Mapa: ¿se refresca solo (JS con `fetch` cada N segundos a un endpoint JSON del web) o recargando la página? Si es con JS, el endpoint JSON también tiene que pasar por el middleware de sesión.

## 5. Dependencias con el resto
- **Recibe de A:** la Tarea 0 (layout, navbar, sesión, middleware) y la convención de plantillas. Para la parte de pantallas, tomarla de `entrega-4` una vez pusheada.
- **Comparte con A:** las pantallas 13 y 14 se cuelgan del menú de la entidad (A agrega los links o B los agrega en el navbar).
- **Comparte con C:** si C cambia los DTOs de donaciones al pasar a gRPC, avisar; los endpoints REST que usa el web no deberían cambiar.
- **Archivos compartidos:** `persistence.xml` (donaciones), `Clases.puml`, `navbar.hbs`, `styles.css`. Regla: agregar al final, en una sección propia.

## 6. Entregables de documentación
- [ ] Bocetos de las pantallas 5–9, 13 y 14 en `documents/bocetos/`, **antes** de implementarlas.
- [ ] Clases de gamificación en `Clases.puml` (paquete propio).
- [ ] Justificación del modelo de incentivos (por qué estrategia o evento, cómo se extiende con nuevas misiones).

## 7. Cronograma
**[COMPLETAR]** Fechas por bloque. Sugerido: días 1–2 gamificación + bocetos; días 3–4 endpoints de notificaciones, seguimiento y filtros; después las pantallas.

Rama: todo en `entrega-4` hasta que esté terminado el trabajo de todo el equipo.
