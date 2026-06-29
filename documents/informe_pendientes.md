# Informe de Requerimientos Pendientes - Entrega 2 (DonaTrack)

Este informe detalla las funcionalidades, controladores, servicios y tareas programadas que faltan implementar en los microservicios de **Donaciones** y **Logística** para cumplir con la consigna de la Entrega 2 del Trabajo Práctico de Diseño de Sistemas de Información (DSI).

---

## Módulo 1: Donaciones - Trazabilidad y APIs de Negocio

### A. CRUD de Donaciones y Trazabilidad Manual
* **Qué requiere la consigna:** Operaciones CRUD sobre las donaciones y garantizar la trazabilidad y auditoría de sus estados (historial de cambios de estado).
* **Qué falta actualmente:** Falta implementar el controlador básico para la gestión individual de donaciones y la capacidad de realizar cambios de estado manuales y auditables por parte del administrador.
* **Cómo implementarlo:**
  1. **Crear `DonacionController`** en el microservicio de Donaciones:
     * `GET /api/donaciones`: Obtener todas las donaciones.
     * `GET /api/donaciones/{id}`: Obtener el detalle de una donación.
     * `POST /api/donaciones`: Crear/ingresar una donación (estado inicial `EN_DEPOSITO`).
     * `DELETE /api/donaciones/{id}`: Eliminar una donación.
  2. **Endpoint de cambio de estado auditado:**
     * `PUT /api/donaciones/{id}/estado`
     * **Request Body:** `{ "estado": "VENCIDA", "justificacion": "Control de calidad descartó el lote por fecha de vencimiento." }`
     * **Lógica:** El controlador debe obtener la donación, llamar a `donacion.cambiarEstado(nuevoEstado, justificacion)` (lo que inserta el registro en la lista de `historialEstados` de la donación con la fecha y hora actual) y guardar los cambios.

### B. CRUD de Donantes (Completado)
* **Estado:** Se implementó exitosamente el [DonanteController](file:///d:/Usuario/Desktop/Estudio/UTN/3%20Año/DSI/TPA/dsi-tpa-2026-DonaTrack-g8/donaciones/src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonanteController.java) que gestiona de manera polimórfica los endpoints para `PersonaHumana` y `PersonaJuridica`.

---

## Módulo 2: Eventos e Integración de Notificaciones (Asincronismo)

### A. Tarea Programada de Inactividad de Donantes
* **Qué requiere la consigna:** Enviar notificaciones de incentivo a los donantes que no registren interacción con la plataforma durante más de 20 días.
* **Qué falta actualmente:** Falta el proceso automatizado (Scheduler) que detecte a estos donantes.
* **Cómo implementarlo:**
  1. En el microservicio de Donaciones, crear un servicio de tareas programadas (ej: `DonanteSchedulerService`).
  2. Implementar un método ejecutado en horarios de baja carga usando `@Scheduled(cron = "0 0 1 * * *")` (ejecución diaria a la 1:00 AM).
  3. **Lógica de detección:**
     * Recupera todos los donantes de `DonanteRepository`.
     * Filtra los donantes comparando la fecha de su última interacción/donación (`donante.getFechaUltimaDonacion()`) con la fecha actual. Si la diferencia es mayor a 20 días, invoca a `eventoService.notificarAusenciaDonante(donante)`.
     * El evento se emite al `EventManager` para que envíe la notificación real (Email/SMS/WhatsApp).

### B. Notificación de Entrega Fallida
* **Qué requiere la consigna:** Notificar a la entidad, al donante y a los administradores si la entrega no pudo concretarse (vencimiento, imposibilidad de recepción, incidentes).
* **Qué falta actualmente:** `EventoService` tiene implementados los eventos de inicio de ruta y entrega exitosa, pero le falta la gestión de la entrega fallida.
* **Cómo implementarlo:**
  1. Agregar un método `notificarEntregaFallida(Long donacionId, String motivo)` en `EventoService`.
  2. Emitir eventos del tipo `TipoEvento.ENTREGA_FALLIDA_DONANTE`, `TipoEvento.ENTREGA_FALLIDA_ENTIDAD` y `TipoEvento.ENTREGA_FALLIDA_ADMIN`.
  3. Este método debe ser invocado cuando la entidad beneficiaria rechaza la entrega desde su controlador (`/api/entidad-beneficiaria/{entidadId}/entregas/{donacionId}/no-recibida`).

---

## Módulo 3: Logística - Planificación de Rutas (Integración Externa y Lotes)

### A. Cliente de Integración Asincrónica por Lotes
* **Qué requiere la consigna:** En horarios de baja carga, generar planes de ruta nocturnos usando el optimizador de terceros. Debe ser en lotes de hasta 100 donaciones y soportar callback asincrónico.
* **Qué falta actualmente:** Falta la lógica de integración externa y la secuencia recursiva asincrónica para camiones sobrantes.
* **Cómo implementarlo:**
  1. **Implementar `AdapterPlanificadorExterno`:** Un cliente REST que haga un POST asincrónico al servicio externo con el lote de donaciones y los camiones disponibles, enviándole una URL de callback dinámica que contenga el identificador de la ejecución o de los camiones involucrados.
  2. **Configurar el Receptor de Callback en Logística:**
     * `POST /api/logistica/planificacion/callback/{procesoId}` (en `PlanificacionRutasController`).
  3. **Implementar la secuencia reactiva (Pipeline de Lotes):**
     * Al recibir la respuesta del callback:
       1. Registra las rutas generadas para los camiones asignados.
       2. Llama al microservicio de Donaciones (`PUT /api/donaciones/actualizar-estados-planificacion`) enviando la lista de donaciones ruteadas con éxito para que pasen a estado `LISTA_PARA_ENTREGAR`.
       3. Verifica en Logística si quedan camiones disponibles sin ruta asignada para el día de mañana.
       4. Si quedan camiones libres, vuelve a pedir a Donaciones las siguientes 100 donaciones en estado `ASIGNACION_REALIZADA` (`GET /api/donaciones?estado=ASIGNACION_REALIZADA&limit=100`) y repite el proceso de forma recursiva asíncrona.

---

## Módulo 4: Logística - Monitoreo de Camiones en Tiempo Real

### A. Receptor de Ubicación de Camiones
* **Qué requiere la consigna:** Mostrar en tiempo real la posición de los camiones y su avance sobre la ruta activa. Se debe elegir una alternativa de integración (se recomienda la **Opción 1: Dispositivo GPS configurable en el camión** por simplicidad y robustez de hardware).
* **Qué falta actualmente:** No hay endpoints ni lógica para recibir o almacenar la geolocalización de los camiones.
* **Cómo implementarlo:**
  1. **Crear el endpoint de telemetría** en Logística:
     * `POST /api/logistica/camiones/{patente}/ubicacion`
     * **Request Body:** `{ "latitud": -34.5984, "longitud": -58.4201, "velocidad": 45.0, "timestamp": "2026-06-29T19:40:00Z" }`
  2. **Lógica de negocio:**
     * Busca la ruta activa para el camión con esa patente.
     * Si la ruta está en estado `EN_TRASLADO`, agrega una nueva instancia de la clase `Ubicacion` al historial de ubicaciones de la ruta o del camión.
     * Guarda la información para que esté disponible en tiempo real.

### B. Dashboard de Monitoreo
* **Qué requiere la consigna:** Mostrar el avance de los camiones en tiempo real.
* **Cómo implementarlo:**
  * Implementar un endpoint `GET /api/logistica/monitoreo/activos` que retorne una lista de los camiones que actualmente tienen rutas en estado `EN_TRASLADO`, incluyendo su patente, última ubicación registrada (latitud/longitud), velocidad y la lista de paradas pendientes de su ruta actual.

---

## Módulo 5: Logística - CRUDs de Soporte

### A. CRUD de Flota de Camiones
* **Qué requiere la consigna:** Gestión de la flota de camiones.
* **Qué falta actualmente:** Falta el controlador. El repositorio `CamionRepository` ya está listo.
* **Cómo implementarlo:**
  * Crear `CamionController` en Logística:
    * `GET /api/camiones`: Listado de todos los camiones.
    * `GET /api/camiones/{id}`: Detalle de un camión.
    * `POST /api/camiones`: Registrar un camión.
    * `PUT /api/camiones/{id}`: Modificar los atributos del camión (patente, volumen, peso, etc.).
    * `DELETE /api/camiones/{id}`: Eliminar un camión.

### B. CRUD de Rutas y Entregas
* **Qué requiere la consigna:** Operaciones CRUD sobre las rutas y entregas.
* **Cómo implementarlo:**
  * Crear `RutaController` en Logística:
    * `GET /api/rutas`: Obtener las rutas planificadas para un día en particular.
    * `GET /api/rutas/{id}`: Obtener el detalle de una ruta con sus paradas y entregas.
    * `DELETE /api/rutas/{id}`: Cancelar o eliminar una ruta planificada antes de que se inicie.
