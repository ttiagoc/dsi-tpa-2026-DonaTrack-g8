# Tareas Pendientes: Refactorización de Controllers y DTOs

## Microservicio Donaciones
- [x] Refactorizar `EventoController` (Base path: `/donaciones/evento`)
  - Crear DTOs en `dto/evento/`: `InicioRutaRequest`, `InicioRutaResponse`, `ConfirmacionEntregaExitosaRequest`, `ConfirmacionEntregaExitosaResponse`, `ParadaInfo`.
  - Eliminar DTOs antiguos que son reemplazados: `dto/EntregaExitosaDTO.java`, `dto/InicioRutaDTO.java`, `dto/ParadaDTO.java`.
  - Actualizar `EventoController` para usar los records y quitar lógica `try/catch`.
  - Actualizar `EventoService` para procesar/devolver los records.
- [x] Refactorizar `MatchmakingController` (Base path: `/donaciones/matchmaking`)
  - Crear DTOs en `dto/matchmaking/`: `ObtenerPropuestasPendientesResponse`, `AceptarPropuestaResponse`, `RechazarPropuestaResponse`, `ForzarEjecucionMatchmakingResponse`, `PropuestaMatchmakingInfo`.
  - Mover la inyección directa de `ResultadoMatchmakingRepository` desde el controller al `MatchmakingService`.
  - Actualizar `MatchmakingController` y `MatchmakingService` con los records y el nuevo base path.

## Microservicio Logística
- [x] Refactorizar `CamionController` (Base path: `/logistica/camiones`)
  - Crear DTOs en `dto/camion/`: `ObtenerTodosCamionesResponse`, `ObtenerCamionResponse`, `CrearCamionRequest`, `CrearCamionResponse`, `ActualizarCamionRequest`, `ActualizarCamionResponse`, `ChoferInfo`.
  - Eliminar DTOs antiguos: `dto/CamionDTO.java`.
  - Actualizar Controller y Service.
- [x] Refactorizar `RutaController` (Base path: `/logistica/rutas`)
  - Crear DTOs en `dto/ruta/`: `ObtenerTodasRutasResponse`, `ObtenerRutaResponse`, `CrearRutaRequest`, `CrearRutaResponse`, `ActualizarRutaRequest`, `ActualizarRutaResponse`.
  - Eliminar DTOs antiguos si corresponde (`dto/RutaDTO.java`).
  - Actualizar Controller y Service.
- [x] Refactorizar `EntregaDonacionesController` (Base path: `/logistica/entregas`)
  - Crear DTOs en `dto/entregadonaciones/`: `IniciarRutaResponse`, `ConfirmarEntregaExitosaResponse`.
  - Eliminar DTOs antiguos si corresponde (`dto/EntregaExitosaDTO.java`, `dto/InicioRutaDTO.java`).
  - Actualizar Controller y Service.
- [x] Refactorizar `MonitoreoController` (Base path: `/logistica/monitoreo`)
  - Crear DTOs en `dto/monitoreo/`: `RecibirTelemetriaRequest`, `ObtenerUbicacionResponse`.
  - Cambiar el path base si aplica y actualizar Controller y Service.
- [x] Refactorizar `PlanificacionRutasController` (Base path: `/logistica/planificacion`)
  - Crear DTOs en `dto/planificacion/`: `EjecutarPlanificacionRequest`, `CamionPlanificacionInfo`, `DireccionInfo`.
  - Eliminar DTOs antiguos: `dto/ResultadoPlanificacionDTO.java`, `dto/DireccionDTO.java`.
  - Actualizar Controller y Service.
- [ ] Refactorizar `RutaController` (Base path: `/logistica/rutas`)
  - Crear DTOs en `dto/ruta/`: `ObtenerTodasRutasResponse`, `ObtenerRutaResponse`, `RutaInfo`, `ParadaInfo` (puede reemplazarse `dto/ParadaDTO.java`).
  - Actualizar Controller y Service.
