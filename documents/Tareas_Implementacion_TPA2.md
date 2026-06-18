# Tareas de Implementación - Entrega 2

A continuación se detalla la lista de tareas a realizar para programar el 100% de los requerimientos de la segunda entrega. Para cada funcionalidad, se especifica qué archivos físicos (.java) se van a crear y a qué clases del **Diagrama de Clases UML** están representando.

---

## 1. Motor de Matchmaking y Algoritmos (Donaciones)
**Requisito:** Ejecutar asincrónicamente en horarios de baja carga los algoritmos de asignación de necesidades.
*   **Clases relacionadas en el Diagrama:** `MotorDeMatchmaking`, `AlgoritmoAsignacion`, `CompatibilidadSemantica`, `PrioridadSubAtendidos`.
*   **Archivos a crear (en `donaciones-service`):**
    *   `MotorDeMatchmakingService.java`: El servicio principal de orquestación.
    *   `AlgoritmoAsignacion.java`: Interfaz de diseño Strategy.
    *   `CompatibilidadSemantica.java` y `PrioridadSubAtendidos.java`: Implementaciones concretas de la interfaz.
    *   `MatchmakingJob.java`: Clase de configuración Spring con un método `@Scheduled` nocturno que buscará las donaciones `EN_DEPOSITO` y llamará al motor.

---

## 2. Planificación de Rutas y Fraccionamiento (Logística)
**Requisito:** Mandar las donaciones a un planificador externo en lotes de máximo 100 cajas, asincrónicamente.
*   **Clases relacionadas en el Diagrama:** `GestorPlanificacionRutas`, `AdapterPlanificadorExterno`, `ReceptorPlanificacionRutas`, `ResultadoPlanificacionDTO`.
*   **Archivos a crear (en `logistica-service`):**
    *   `GestorPlanificacionRutasService.java`: Se encargará de consultar las donaciones aprobadas (vía REST) y partir la lista usando particiones de 100 elementos.
    *   `PlanificadorExternoClient.java`: Representará al `AdapterPlanificadorExterno` del diagrama (usando `RestTemplate` o `FeignClient`).
    *   `PlanificacionJob.java`: Tarea `@Scheduled` nocturna para iniciar el proceso de planificación.

---

## 3. Webhook de Respuesta del Planificador Externo
**Requisito:** Exponer una URL de Callback para recibir los planes de ruteo y las donaciones sin asignar.
*   **Clases relacionadas en el Diagrama:** `ReceptorPlanificacionRutas`, `ResultadoPlanificacionDTO`, `Ruta`.
*   **Archivos a crear (en `logistica-service`):**
    *   `PlanificacionCallbackController.java`: (Cumple la función del `Receptor` del diagrama). Expondrá el endpoint `POST /api/planificacion/callback`.
    *   `ResultadoPlanificacionDTO.java`: Objeto Java para poder leer el JSON de respuesta que manda el servicio externo.

---

## 4. Trazabilidad y Tracking en Tiempo Real
**Requisito:** Poder iniciar rutas, terminarlas, y que la App Móvil del conductor envíe su ubicación en vivo.
*   **Clases relacionadas en el Diagrama:** `Ruta`, `EstadoRuta`, `Ubicacion`.
*   **Archivos a modificar/crear (en `logistica-service`):**
    *   `RutaController.java`: Añadir endpoint `PATCH /api/rutas/{id}/iniciar` para pasar el estado a `EN_TRASLADO`.
    *   `UbicacionController.java` (o `TrackingController`): Añadir endpoint `POST /api/camiones/{id}/ubicacion` para recibir las latitudes y longitudes de los conductores.
    *   `UbicacionDTO.java`: Para deserializar los payloads del celular.

---

## 5. Eventos y Notificaciones Reales
**Requisito:** Mandar alertas de inactividad, inicio de ruta y entregas.
*   **Clases relacionadas en el Diagrama:** `EventManager`, `TipoEvento`, `Listener`, `NotificacionesAdapter`.
*   **Archivos a crear (en ambos servicios y `common-lib`):**
    *   `InactividadDonanteJob.java` (en `donaciones-service`): Tarea `@Scheduled` que detecte >20 días sin donar.
    *   `EntregaConfirmadaController.java`: Endpoint para que la entidad notifique que recibió la caja.
    *   Modificar controladores (`Matchmaking`, `Ruta`, etc.) para que consuman el `NotificacionService` enviando correos reales cuando haya cambios de estado, inyectando el componente compartido.
