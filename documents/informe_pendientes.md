# Informe de Requerimientos Pendientes - Entrega 2 (DonaTrack)

Este informe detalla las funcionalidades, controladores, servicios y tareas programadas que faltan implementar en los microservicios de **Donaciones** y **Logística** para cumplir con la consigna de la Entrega 2 del Trabajo Práctico de Diseño de Sistemas de Información (DSI).

> **Última actualización:** 29/06/2026

---

## Módulo 1: Donaciones - Trazabilidad y APIs de Negocio

### A. CRUD de Donaciones y Trazabilidad Manual — ✅ Completado
* **Qué requiere la consigna:** Operaciones CRUD sobre las donaciones y garantizar la trazabilidad y auditoría de sus estados (historial de cambios de estado).
* **Estado actual:** Implementado en [DonacionController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java) y [DonacionService](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java).
  * `GET /api/donacion` — Obtener todas las donaciones. ✅
  * `GET /api/donacion/{id}` — Obtener detalle. ✅
  * `POST /api/donacion` — Crear donación. ✅
  * `DELETE /api/donacion/{id}` — Eliminar donación. ✅
  * `PUT /api/donacion/estado` — Cambio de estado auditado con justificación. ✅
  * `GET /api/donacion/asignadas` — Obtener donaciones asignadas (para integración con Logística). ✅
  * `POST /api/donacion/lista-entrega` — Marcar donaciones como listas para entregar. ✅

### B. CRUD de Donantes — ✅ Completado
* **Estado:** Implementado en [DonanteController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonanteController.java) con gestión polimórfica de `PersonaHumana` y `PersonaJuridica`.

### C. CRUD de Entidades Beneficiarias y Necesidades — ✅ Completado
* **Estado:** Implementado en [EntidadBenficiariaController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/EntidadBenficiariaController.java).
  * `GET /api/entidad-beneficiaria` — Listar todas. ✅
  * `GET /api/entidad-beneficiaria/{id}` — Detalle. ✅
  * `POST /api/entidad-beneficiaria` — Crear. ✅
  * `PUT /api/entidad-beneficiaria/{id}` — Actualizar. ✅
  * `GET /api/entidad-beneficiaria/{entidadId}/necesidades` — Listar necesidades. ✅
  * `POST /api/entidad-beneficiaria/{entidadId}/necesidades` — Registrar necesidad. ✅
  * `DELETE /api/entidad-beneficiaria/{entidadId}/necesidades/{subcategoriaNombre}` — Eliminar necesidad. ✅
  * `POST /api/entidad-beneficiaria/{entidadId}/entregas/{donacionId}/confirmar` — Confirmar entrega. ✅
  * `POST /api/entidad-beneficiaria/{entidadId}/entregas/{donacionId}/no-recibida` — Reportar no recibida. ✅

### D. Matchmaking (Asignación de Donaciones) — ✅ Completado
* **Estado:** Implementado en [MatchmakingController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/MatchmakingController.java) y [MatchmakingService](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/MatchmakingService.java).
  * Proceso nocturno `@Scheduled(cron = "0 0 3 * * *")` con ambos algoritmos (Compatibilidad Semántica + Prioridad Sub-atendidos). ✅
  * `GET /api/matchmaking/pendientes` — Ver propuestas pendientes de revisión. ✅
  * `POST /api/matchmaking/propuestas/{id}/aceptar` — Aceptar propuesta y asignar entidad. ✅
  * `POST /api/matchmaking/propuestas/{id}/rechazar` — Rechazar propuesta. ✅
  * `POST /api/matchmaking/forzar-ejecucion` — Ejecución a demanda. ✅
  * Notificaciones al donante y a la entidad tras aceptar la propuesta. ✅

---

## Módulo 2: Eventos e Integración de Notificaciones (Asincronismo)

### A. Tarea Programada de Inactividad de Donantes — ✅ Completado
* **Estado:** Implementado en [ControlAusenciaDonantesService](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/ControlAusenciaDonantesService.java).
  * Scheduler `@Scheduled(cron = "0 0 3 * * *")` que detecta donantes inactivos >20 días. ✅
  * Emite evento `AUSENCIA_PLATAFORMA` a través del `EventManager`. ✅

### B. Notificaciones de Inicio de Ruta y Entrega Exitosa — ✅ Completado
* **Estado:** Implementado en [EventoService](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/EventoService.java) y [EventoController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/EventoController.java).
  * `iniciarRuta()` — Notifica a entidades y donantes con eventos `INICIO_RUTA_ENTIDAD` e `INICIO_RUTA_DONANTE`. ✅
  * `confirmarEntregaExitosa()` — Notifica con comprobante (fecha, hora, camión) con eventos `ENTREGA_EXITOSA_DONANTE` y `ENTREGA_EXITOSA_ENTIDAD`. ✅

### C. Notificación de Entrega Fallida — ✅ Completado
* **Estado:** Implementado en [EventoService.notificarEntregaFallida()](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/EventoService.java#L103-L140).
  * Notifica al donante, a la entidad (representantes) y al administrador. ✅
  * Cambia estado de la donación a `ENTREGA_FALLIDA`. ✅
  * Invocado desde `EntidadBenficiariaController` endpoint `no-recibida`. ✅

### D. Integración real con medios de contacto — ✅ Completado
* **Estado:** Implementado en [NotificacionesAdapter](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/notifiaciones/NotificacionesAdapter.java).
  * Envío real por Email, SMS y/o WhatsApp delegado a `MedioContacto.notificar(mensaje)`. ✅

---

## Módulo 3: Logística - Planificación de Rutas (Integración Externa y Lotes)

### A. Planificación Nocturna con Scheduler — ✅ Completado
* **Estado:** Implementado en [PlanificacionRutasService](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/services/PlanificacionRutasService.java).
  * Scheduler `@Scheduled(cron = "0 0 3 * * *")` que pide lotes de hasta 100 donaciones. ✅
  * Consulta a Donaciones por HTTP para obtener donaciones en estado `ASIGNACION_REALIZADA`. ✅
  * Invoca al componente externo simulado ([GestorPlanificacionRutas](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/GestorPlanificacionRutas.java)) de forma asincrónica con `CompletableFuture`. ✅

### B. Receptor de Callback — ✅ Completado
* **Estado:** Implementado en [PlanificacionRutasController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/controllers/PlanificacionRutasController.java).
  * `POST /api/planificacion/confirmacion` — Recibe resultado del planificador externo. ✅
  * Procesa las rutas generadas y actualiza estados en Donaciones (`lista-entrega`). ✅
  * Relanza `planificarRutas()` recursivamente para camiones restantes. ✅

### C. Trazabilidad de Entregas — ✅ Completado
* **Estado:** Implementado en [EntregaDonacionesController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/controllers/EntregaDonacionesController.java) y [EntregaDonacionesService](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/services/EntregaDonacionesService.java).
  * `POST /api/entregas/iniciar/{rutaId}` — El chofer inicia su ruta; cambia estado a `EN_TRASLADO`. ✅
  * `POST /api/entregas/confirmar/{paradaId}/{rutaId}` — Confirma entrega exitosa de una parada. ✅
  * Se notifica al microservicio de Donaciones para cambiar estados y emitir notificaciones. ✅

---

## Módulo 4: Logística - Monitoreo de Camiones en Tiempo Real

### A. Receptor de Ubicación de Camiones — ❌ Pendiente
* **Qué requiere la consigna:** Recibir y almacenar la geolocalización en tiempo real de los camiones (con la alternativa GPS o App Móvil elegida). Mostrar en un dashboard la posición actual y el avance sobre la ruta.
* **Qué falta actualmente:** No existe ningún endpoint para recibir la ubicación GPS de los camiones. La entidad [Ubicacion](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/Ubicacion.java) ya existe y la [Ruta](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/Ruta.java) ya tiene el campo `ultimaUbicacion` con su método `actualizarUbicacion()`, pero no hay controlador ni servicio que lo use.
* **Cómo implementarlo:**
  1. **Crear endpoint de telemetría** en un nuevo controlador (ej: `MonitoreoController`) en Logística:
     * `POST /api/logistica/camiones/{patente}/ubicacion`
     * **Request Body:** `{ "latitud": -34.5984, "longitud": -58.4201, "velocidad": 45.0, "timestamp": "2026-06-29T19:40:00Z" }`
  2. **Lógica de negocio (crear `MonitoreoService`):**
     * Busca el camión por patente usando `CamionRepository.findByPatente()`.
     * Busca la ruta activa del camión usando `RutaRepository.buscarRutasActivasPorCamion()`.
     * Si la ruta está en estado `EN_TRASLADO`, llama a `ruta.actualizarUbicacion(nuevaUbicacion)`.
     * Guarda la ruta actualizada.
  3. **Nota:** La entidad `Ubicacion` actualmente no tiene el campo `velocidad`. Agregar `private Double velocidad;` al modelo.

### B. Dashboard de Monitoreo — ❌ Pendiente
* **Qué requiere la consigna:** Endpoint que permita consultar en tiempo real los camiones activos con su posición, velocidad y paradas pendientes.
* **Qué falta actualmente:** No existe endpoint de monitoreo.
* **Cómo implementarlo:**
  * Agregar en el mismo `MonitoreoController`:
    * `GET /api/logistica/monitoreo/activos`
  * Retorna lista de camiones con rutas en estado `EN_TRASLADO` incluyendo: patente, última ubicación (latitud/longitud), velocidad, y paradas pendientes.
  * Puede usar `RutaRepository.buscarRutasActivas()` que ya existe.

---

## Módulo 5: Logística - CRUDs de Soporte

### A. CRUD de Flota de Camiones — ❌ Pendiente
* **Qué requiere la consigna:** Gestión REST completa de la flota de camiones.
* **Qué falta actualmente:** Falta el controlador. El [CamionRepository](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/repositories/CamionRepository.java) ya está completo con `save`, `findById`, `findByPatente`, `findAll`, `findAllDisponibles` y `deleteById`.
* **Cómo implementarlo:**
  * Crear `CamionController` en Logística:
    * `GET /api/camiones` — Listado de todos los camiones.
    * `GET /api/camiones/{id}` — Detalle de un camión.
    * `POST /api/camiones` — Registrar un camión.
    * `PUT /api/camiones/{id}` — Modificar los atributos del camión (patente, volumen, peso, etc.).
    * `DELETE /api/camiones/{id}` — Eliminar un camión.

### B. CRUD de Rutas y Entregas — ❌ Pendiente
* **Qué requiere la consigna:** Operaciones CRUD sobre las rutas y entregas.
* **Qué falta actualmente:** No existe un controlador CRUD para rutas. El [RutaRepository](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/repositories/RutaRepository.java) ya está completo.
* **Cómo implementarlo:**
  * Crear `RutaController` en Logística:
    * `GET /api/rutas` — Obtener las rutas planificadas (se puede filtrar por fecha con `@RequestParam`).
    * `GET /api/rutas/{id}` — Obtener el detalle de una ruta con sus paradas y entregas.
    * `DELETE /api/rutas/{id}` — Cancelar o eliminar una ruta planificada antes de que se inicie.

---

## Otras Tareas (Errores, Mejoras y Observaciones Detectadas)

### 1. ⚠️ Falta `DELETE` en EntidadBeneficiariaController
* **Problema:** La consigna pide operaciones CRUD completas sobre entidades beneficiarias, pero en [EntidadBenficiariaController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/EntidadBenficiariaController.java) falta el endpoint `DELETE /api/entidad-beneficiaria/{id}` para eliminar una entidad beneficiaria.

### 2. ⚠️ Falta `PUT` para actualizar Necesidades
* **Problema:** La consigna pide CRUD completo sobre necesidades materiales (recurrentes y extraordinarias). Actualmente se pueden listar (`GET`), crear (`POST`) y eliminar (`DELETE`), pero falta `PUT /api/entidad-beneficiaria/{entidadId}/necesidades/{id}` para modificar una necesidad existente.

### 3. ⚠️ Ubicacion no tiene campo `velocidad`
* **Problema:** La consigna requiere que el dispositivo GPS envíe velocidad, pero la entidad [Ubicacion](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/Ubicacion.java) solo tiene `latitud`, `longitud` y `timestamp`. Falta agregar `private Double velocidad;`.

### 4. ⚠️ Typo en nombre del controlador: `EntidadBenficiariaController`
* **Problema:** El nombre del archivo y de la clase es `EntidadBenficiariaController` (falta una "e": debería ser `EntidadBeneficiariaController`). Esto no afecta la funcionalidad pero afecta la legibilidad del código.

### 5. ⚠️ Typo en nombre del paquete: `notifiaciones`
* **Problema:** El paquete de notificaciones se llama `notifiaciones` (falta una "c": debería ser `notificaciones`). Afecta a las clases [EventManager](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/notifiaciones/EventManager.java), [Evento](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/notifiaciones/Evento.java), [Listener](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/notifiaciones/Listener.java) y [NotificacionesAdapter](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/notifiaciones/NotificacionesAdapter.java).

### 6. ⚠️ Ruta de Donaciones para Logística usa path diferente al controller
* **Problema:** En [PlanificacionRutasService.getLote()](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/logistica/src/main/java/ar/edu/utn/frba/ddsi/logistica/services/PlanificacionRutasService.java#L76-L93) se consume la URL `/donaciones/planificadas`, pero el endpoint real en [DonacionController](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java#L32-L40) es `/api/donacion/asignadas`. La URL del path **no coincide**.

### 7. ⚠️ El link del mapa interactivo está hardcodeado como "LINK"
* **Problema:** En [NotificacionesAdapter](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/notifiaciones/NotificacionesAdapter.java#L36-L41), los mensajes de `INICIO_RUTA_DONANTE` e `INICIO_RUTA_ENTIDAD` contienen `"...haciendo click en el siguiente mapa interactivo: LINK"`. Falta reemplazar "LINK" por la URL real del dashboard de monitoreo.

### 8. ⚠️ Entrega fallida no replanifica
* **Problema:** La consigna dice: *"Si la entrega pudiera ser replanificada, se dejará constancia del estado correspondiente en el sistema y podrá generarse una nueva asignación de ruta para la donación en cuestión."* Actualmente [EventoService.notificarEntregaFallida()](file:///c:/Users/pc/Desktop/UTN/2026/DSI/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/EventoService.java#L103-L140) cambia el estado a `ENTREGA_FALLIDA` y notifica, pero no existe lógica ni endpoint para volver a poner la donación en estado `PENDIENTE` (o equivalente) cuando el administrador determina que puede replanificarse.

### 9. ⚠️ Fotos de donación recibida
* **Problema:** La consigna dice: *"La entidad deberá cargar fotos de la donación recibida en la plataforma."* No existe endpoint para subir/asociar fotos a una entrega confirmada.

---

## Resumen de Estado

| Módulo | Componente | Estado |
|--------|-----------|--------|
| Donaciones | CRUD Donaciones + Trazabilidad | ✅ Completado |
| Donaciones | CRUD Donantes | ✅ Completado |
| Donaciones | CRUD Entidades Beneficiarias | ⚠️ Falta DELETE de entidad |
| Donaciones | CRUD Necesidades | ⚠️ Falta PUT de necesidad |
| Donaciones | Matchmaking (Asignación) | ✅ Completado |
| Eventos | Scheduler Inactividad Donantes | ✅ Completado |
| Eventos | Notif. Inicio Ruta + Entrega Exitosa | ✅ Completado |
| Eventos | Notif. Entrega Fallida | ✅ Completado |
| Eventos | Integración real (Email/SMS/WhatsApp) | ✅ Completado |
| Logística | Planificación Nocturna + Callback | ✅ Completado |
| Logística | Trazabilidad de Entregas (chofer) | ✅ Completado |
| **Logística** | **Receptor de Ubicación GPS** | **❌ Pendiente** |
| **Logística** | **Dashboard de Monitoreo** | **❌ Pendiente** |
| **Logística** | **CRUD Camiones** | **❌ Pendiente** |
| **Logística** | **CRUD Rutas** | **❌ Pendiente** |
