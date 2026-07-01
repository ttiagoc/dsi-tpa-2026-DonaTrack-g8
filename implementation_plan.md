# Refactoring de Controllers con DTOs tipados por método

## Descripción

Actualmente los controllers exponen directamente las entidades del dominio (modelos JPA/de negocio) como request/response, y mezclan lógica de control de errores dentro del controller. El objetivo es:

1. Introducir DTOs como **Java records** para cada método (un `...Request` y un `...Response` por operación)
2. Organizar los DTOs en **carpetas por controller** dentro de `dto/`
3. Agregar el **prefijo del microservicio** al path base de cada controller
4. Extraer la lógica de manejo de errores al **service** (el controller solo llama al service)

---

## Decisiones de diseño importantes

> [!IMPORTANT]
> **¿Qué hacemos con los métodos que no reciben body?**
> Los endpoints tipo `GET /{id}` o `DELETE /{id}` no tienen `@RequestBody`, por lo tanto **no necesitan un `...Request` record** (el parámetro ya viene por `@PathVariable`/`@RequestParam`). Solo se crea el `...Response`.

> [!IMPORTANT]
> **¿Qué hacemos con los métodos que retornan `Void`?**
> Algunos métodos retornan `ResponseEntity<Void>` (e.g., `eliminar`, `confirmarEntrega`). En esos casos **no se crea `...Response`** ya que no hay cuerpo. El controller retorna `ResponseEntity.noContent()`.

> [!WARNING]
> **Lógica en el controller:** Actualmente varios controllers tienen `try/catch` para manejar `IllegalArgumentException`. Este manejo se **moverá al service** o se puede manejar con un `@ControllerAdvice`. Por ahora el plan es que el **service lance la excepción** y el controller la propague limpiamente (sin lógica interna). Esto puede hacerse con un `GlobalExceptionHandler`. Si no quieren modificar los services aún, se puede dejar el try/catch pero sin lógica adicional. 
> **Requiere decisión del equipo:** ¿Agregamos un `GlobalExceptionHandler` o dejamos el try/catch por ahora?

> [!NOTE]
> Los DTOs de los controllers de `logistica` van en el paquete `dto/` de `logistica`, y los de `donaciones` van en `dto/` de `donaciones`. Los DTOs que comparten ambos microservicios (ej. `DonacionDTO`) se mantienen separados por microservicio.

---

## Open Questions

> [!IMPORTANT]
> **¿Se actualiza también la firma de los services para que reciban/devuelvan DTOs?**
> El pedido dice "los controllers llaman directamente al service para que implemente la lógica y devuelva el response". Esto implica que el **service debe devolver el DTO response**, no la entidad. ¿Confirmamos que se refactorizan los services también?

Si, estos deben devolver los DTOs correspondientes a cada microservicio.

> [!IMPORTANT]
> **¿Los DTOs existentes (`DonacionDTO`, `EntregaExitosaDTO`, `InicioRutaDTO`, `ParadaDTO`, `CamionDTO`, `ResultadoPlanificacionDTO`) se migran al nuevo formato de records y se mueven a su carpeta correspondiente por controller?** Se asume que sí.

Si.

---

## Cambios propuestos

### MICROSERVICIO: `donaciones`

**Prefijo base del microservicio:** `/donaciones`

---

#### `DonanteController` → base path: `/donaciones/donantes`

##### Nuevos DTOs en `dto/donante/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `GET /` | Response | `ObtenerTodosDonanteResponse` | `Long id, String tipo, String nombre` |
| `GET /{id}` | Response | `ObtenerDonanteResponse` | `Long id, String tipo, String nombre, List<MedioContactoInfo> contactos` |
| `POST /persona-humana` | Request | `CrearPersonaHumanaRequest` | `String nombre, String apellido, LocalDate fechaNacimiento, String dni, String genero, String direccion, List<MedioContactoInfo> contactos` |
| `POST /persona-humana` | Response | `CrearPersonaHumanaResponse` | `Long id, String nombre, String apellido, String dni` |
| `POST /persona-juridica` | Request | `CrearPersonaJuridicaRequest` | `String razonSocial, String rubro, String tipo, String cuit, List<RepresentanteInfo> representantes` |
| `POST /persona-juridica` | Response | `CrearPersonaJuridicaResponse` | `Long id, String razonSocial, String cuit` |
| `PUT /persona-humana/{id}` | Request | `ActualizarPersonaHumanaRequest` | `String nombre, String apellido, LocalDate fechaNacimiento, String dni, String genero, String direccion` |
| `PUT /persona-humana/{id}` | Response | `ActualizarPersonaHumanaResponse` | `Long id, String nombre, String apellido, String dni` |
| `PUT /persona-juridica/{id}` | Request | `ActualizarPersonaJuridicaRequest` | `String razonSocial, String rubro, String tipo, String cuit` |
| `PUT /persona-juridica/{id}` | Response | `ActualizarPersonaJuridicaResponse` | `Long id, String razonSocial, String cuit` |
| `DELETE /{id}` | — | (sin body, sin response) | — |

Además se necesitan records auxiliares (pueden ir en el mismo paquete):
- `MedioContactoInfo` (para representar contactos: `String tipo, String valor`)
- `RepresentanteInfo` (`String nombre, String apellido, String correo`)

---

#### `DonacionController` → base path: `/donaciones/donacion`

##### Nuevos DTOs en `dto/donacion/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `GET /` | Response | `ObtenerTodasDonacionesResponse` | `Long id, String subcategoria, String estadoActual, LocalDateTime fecha` |
| `GET /{id}` | Response | `ObtenerDonacionResponse` | `Long id, String subcategoria, String estadoBienes, List<BienInfo> bienes, String estadoActual, LocalDateTime fecha, Long donanteId, Long entidadAsignadaId` |
| `POST /` | Request | `CrearDonacionRequest` | `String descripcion, LocalDateTime fecha, List<BienInfo> bienes` |
| `POST /` | Response | `CrearDonacionResponse` | `List<DonacionCreadaInfo> donaciones` |
| `DELETE /{id}` | — | (sin body, sin response) | — |
| `PUT /estado/{id}` | Request | `CambiarEstadoDonacionRequest` | `String estado, String justificacion` |
| `PUT /estado/{id}` | Response | `CambiarEstadoDonacionResponse` | `Long id, String estadoActual, LocalDateTime fechaCambio` |
| `GET /asignadas` | Response | `ObtenerDonacionesAsignadasResponse` | `List<DonacionAsignadaInfo> donaciones` |
| `POST /lista-entrega` | Request | `DonacionesListaEntregaRequest` | `List<DonacionEntregaInfo> donaciones` |
| `PUT /{id}/replanificar` | Response | `ReplanificarDonacionResponse` | `Long id, String estadoActual` |

Records auxiliares en el mismo paquete:
- `BienInfo` (`String descripcion, Long cantidad, Double pesoKgPorUnidad, Double volumenM3PorUnidad, String subcategoria, String estadoBien, LocalDate fechaVencimiento`)
- `DonacionCreadaInfo` (`Long id, String subcategoria, String estadoActual`)
- `DonacionAsignadaInfo` (`Long id, Double peso, Double volumen, String direccion`)
- `DonacionEntregaInfo` (`Long id, Double peso, Double volumen, String direccion`)

> [!NOTE]
> Los actuales `DonacionDTO` en `dto/` (raíz) y `dto/donacion/` (vacío) se reemplazan con los records en `dto/donacion/`.

---

#### `EntidadBeneficiariaController` → base path: `/donaciones/entidad-beneficiaria`

##### Nuevos DTOs en `dto/entidadbeneficiaria/`

> [!NOTE]
> Ya existen algunos DTOs en esta carpeta (`CrearEntidadBeneficiariaRequest`, `ActualizarEntidadBeneficiariaRequest`, `EntidadBeneficiariaResponse`, `NecesidadResponse`). Se **migran a records** y se completan los faltantes.

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `GET /` | Response | `ObtenerTodasEntidadesResponse` | `Long id, String razonSocial, String direccion` |
| `GET /{id}` | Response | `ObtenerEntidadResponse` | `Long id, String razonSocial, String direccion, String telefono, List<String> correoRepresentantes, List<NecesidadInfo> necesidades` |
| `POST /` | Request | `CrearEntidadBeneficiariaRequest` | `String razonSocial, String direccion, String telefono, List<String> correoRepresentantes` |
| `POST /` | Response | `CrearEntidadBeneficiariaResponse` | `Long id, String razonSocial` |
| `DELETE /{id}` | — | (sin body, sin response) | — |
| `PUT /{id}` | Request | `ActualizarEntidadBeneficiariaRequest` | `String razonSocial, String direccion, String telefono, List<String> correoRepresentantes` |
| `PUT /{id}` | Response | `ActualizarEntidadBeneficiariaResponse` | `Long id, String razonSocial, String direccion` |
| `GET /{entidadId}/necesidades` | Response | `ObtenerNecesidadesResponse` | `List<NecesidadInfo> necesidades` |
| `POST /{entidadId}/necesidades` | Request | `RegistrarNecesidadRequest` | `String subcategoria, String tipoNecesidad, String descripcion, Long cantidad` |
| `POST /{entidadId}/necesidades` | Response | `RegistrarNecesidadResponse` | `Long id, String descripcion, Long cantidad, String tipoNecesidad` |
| `DELETE /{entidadId}/necesidades/{necesidadId}` | — | (sin body, sin response) | — |
| `POST /{entidadId}/entregas/{donacionId}/confirmar` | — | (sin body, sin response) | — |
| `POST /{entidadId}/entregas/{donacionId}/no-recibida` | Request | `ReportarNoRecibidaRequest` | `String motivo` (antes era `@RequestParam`) |
| `POST /{entidadId}/entregas/{donacionId}/fotos` | Request | `SubirFotosRecepcionRequest` | `List<String> fotosUrl` |

Records auxiliares: `NecesidadInfo` (`Long id, String subcategoria, String tipoNecesidad, String descripcion, Long cantidad`)

---

#### `EventoController` → base path: `/donaciones/evento`

##### Nuevos DTOs en `dto/evento/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `POST /inicio-ruta` | Request | `InicioRutaRequest` | `Long rutaId, List<ParadaInfo> paradas` |
| `POST /inicio-ruta` | Response | `InicioRutaResponse` | `String mensaje` |
| `POST /confirmacion-entrega-exitosa` | Request | `ConfirmacionEntregaExitosaRequest` | `Long entidadId, List<Long> donacionIds, String patenteCamion, LocalDateTime fechaHora` |
| `POST /confirmacion-entrega-exitosa` | Response | `ConfirmacionEntregaExitosaResponse` | `String mensaje` |

Records auxiliares: `ParadaInfo` (`Long entidadId, List<Long> donacionIds`)

> [!NOTE]
> Los actuales `InicioRutaDTO` y `EntregaExitosaDTO` en `dto/` raíz de donaciones se reemplazan por estos records.

---

#### `MatchmakingController` → base path: `/donaciones/matchmaking`

##### Nuevos DTOs en `dto/matchmaking/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `GET /pendientes` | Response | `ObtenerPropuestasPendientesResponse` | `List<PropuestaMatchmakingInfo> propuestas` |
| `POST /propuestas/{id}/aceptar` | — | (solo `@RequestParam Long entidadId`) | — |
| `POST /propuestas/{id}/aceptar` | Response | `AceptarPropuestaResponse` | `String mensaje` |
| `POST /propuestas/{id}/rechazar` | Response | `RechazarPropuestaResponse` | `String mensaje` |
| `POST /forzar-ejecucion` | Response | `ForzarEjecucionMatchmakingResponse` | `String mensaje` |

Records auxiliares: `PropuestaMatchmakingInfo` (`Long id, Long donacionId, List<Long> entidadesSugeridасIds, LocalDateTime fechaEjecucion, String estado`)

> [!NOTE]
> `MatchmakingController` actualmente inyecta directamente `ResultadoMatchmakingRepository`. **Debe eliminarse**: la lógica de obtener pendientes se mueve a `MatchmakingService`.

---

### MICROSERVICIO: `logistica`

**Prefijo base del microservicio:** `/logistica`

---

#### `CamionController` → base path: `/logistica/camiones`

##### Nuevos DTOs en `dto/camion/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `GET /` | Response | `ObtenerTodosCamionesResponse` | `Long id, String patente, Double capacidadVolumen, Double capacidadCarga` |
| `GET /{id}` | Response | `ObtenerCamionResponse` | `Long id, String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, ChoferInfo chofer` |
| `POST /` | Request | `CrearCamionRequest` | `String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, ChoferInfo chofer` |
| `POST /` | Response | `CrearCamionResponse` | `Long id, String patente` |
| `PUT /{id}` | Request | `ActualizarCamionRequest` | `String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, ChoferInfo chofer` |
| `PUT /{id}` | Response | `ActualizarCamionResponse` | `Long id, String patente` |
| `DELETE /{id}` | — | (sin body, sin response) | — |

Records auxiliares: `ChoferInfo` (`String nombre, String apellido`)

---

#### `EntregaDonacionesController` → base path: `/logistica/entregas`

##### Nuevos DTOs en `dto/entregadonaciones/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `POST /iniciar/{rutaId}` | Response | `IniciarRutaResponse` | (puede retornar vacío o `String mensaje`) |
| `POST /confirmar/{paradaId}/{rutaId}` | Response | `ConfirmarEntregaExitosaResponse` | (puede retornar vacío o `String mensaje`) |

> [!NOTE]
> Estos métodos actualmente devuelven `ResponseEntity<Void>`. Si se desea mantener así, no se necesita Response DTO. Se puede optar por devolver una confirmación con mensaje.

---

#### `MonitoreoController` → base path: `/logistica/monitoreo`

> [!NOTE]
> Ya tiene el prefijo `/api/logistica/monitoreo`. Solo hay que ajustar a `/logistica/monitoreo`.

##### Nuevos DTOs en `dto/monitoreo/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `POST /ubicacion/{patente}` | Request | `RecibirTelemetriaRequest` | `Double latitud, Double longitud, LocalDateTime timestamp, Double velocidad` |
| `GET /ubicacion/{rutaId}` | Response | `ObtenerUbicacionResponse` | `Double latitud, Double longitud, LocalDateTime timestamp, Double velocidad` |

---

#### `PlanificacionRutasController` → base path: `/logistica/planificacion`

##### Nuevos DTOs en `dto/planificacion/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `POST /confirmacion` | Request | `EjecutarPlanificacionRequest` | `List<CamionPlanificacionInfo> camiones, List<Long> donacionesSinAsignar` |

Records auxiliares: `CamionPlanificacionInfo` (`Long id, List<DireccionInfo> direcciones`), `DireccionInfo` (`String direccion`)

> [!NOTE]
> El actual `ResultadoPlanificacionDTO` se reemplaza por `EjecutarPlanificacionRequest`.

---

#### `RutaController` → base path: `/logistica/rutas`

##### Nuevos DTOs en `dto/ruta/`

| Método | Tipo | Record | Campos |
|--------|------|--------|--------|
| `GET /` | Response | `ObtenerTodasRutasResponse` | `List<RutaInfo> rutas` |
| `GET /{id}` | Response | `ObtenerRutaResponse` | `Long id, LocalDate fecha, String estado, Long camionId, String patenteCamion, List<ParadaInfo> paradas` |
| `DELETE /{id}` | — | (sin body, sin response) | — |

Records auxiliares: `RutaInfo` (`Long id, LocalDate fecha, String estado`), `ParadaInfo` (`Integer orden, String destino, Long entidad, List<Long> entregas`)

---

## Archivos a eliminar (reemplazados por los nuevos records)

### `donaciones`
- [DELETE] `dto/DonacionDTO.java` → reemplazado por records en `dto/donacion/`
- [DELETE] `dto/EntregaExitosaDTO.java` → reemplazado por `dto/evento/ConfirmacionEntregaExitosaRequest.java`
- [DELETE] `dto/InicioRutaDTO.java` → reemplazado por `dto/evento/InicioRutaRequest.java`
- [DELETE] `dto/ParadaDTO.java` → reemplazado por `dto/evento/ParadaInfo.java`
- [DELETE] `dto/entidadbeneficiaria/EntidadBeneficiariaResponse.java` → reemplazado por records
- [DELETE] `dto/entidadbeneficiaria/CrearEntidadBeneficiariaRequest.java` → reemplazado por record
- [DELETE] `dto/entidadbeneficiaria/ActualizarEntidadBeneficiariaRequest.java` → reemplazado por record
- [DELETE] `dto/entidadbeneficiaria/NecesidadResponse.java` → reemplazado por record

### `logistica`
- [DELETE] `dto/CamionDTO.java` → reemplazado por `dto/planificacion/CamionPlanificacionInfo.java`
- [DELETE] `dto/DireccionDTO.java` → reemplazado por `dto/planificacion/DireccionInfo.java`
- [DELETE] `dto/DonacionDTO.java` → ya no necesario (lógica de asignación usa IDs)
- [DELETE] `dto/EntregaExitosaDTO.java` → ya no necesario
- [DELETE] `dto/InicioRutaDTO.java` → ya no necesario
- [DELETE] `dto/ParadaDTO.java` → reemplazado por record
- [DELETE] `dto/ResultadoPlanificacionDTO.java` → reemplazado por `dto/planificacion/EjecutarPlanificacionRequest.java`

---

## Estructura final de carpetas de DTOs

```
donaciones/dto/
├── donante/
│   ├── ObtenerTodosDonanteResponse.java     (record)
│   ├── ObtenerDonanteResponse.java          (record)
│   ├── CrearPersonaHumanaRequest.java       (record)
│   ├── CrearPersonaHumanaResponse.java      (record)
│   ├── CrearPersonaJuridicaRequest.java     (record)
│   ├── CrearPersonaJuridicaResponse.java    (record)
│   ├── ActualizarPersonaHumanaRequest.java  (record)
│   ├── ActualizarPersonaHumanaResponse.java (record)
│   ├── ActualizarPersonaJuridicaRequest.java  (record)
│   ├── ActualizarPersonaJuridicaResponse.java (record)
│   ├── MedioContactoInfo.java               (record aux)
│   └── RepresentanteInfo.java               (record aux)
├── donacion/
│   ├── ObtenerTodasDonacionesResponse.java  (record)
│   ├── ObtenerDonacionResponse.java         (record)
│   ├── CrearDonacionRequest.java            (record)
│   ├── CrearDonacionResponse.java           (record)
│   ├── CambiarEstadoDonacionRequest.java    (record)
│   ├── CambiarEstadoDonacionResponse.java   (record)
│   ├── ObtenerDonacionesAsignadasResponse.java (record)
│   ├── DonacionesListaEntregaRequest.java   (record)
│   ├── ReplanificarDonacionResponse.java    (record)
│   ├── BienInfo.java                        (record aux)
│   ├── DonacionCreadaInfo.java              (record aux)
│   ├── DonacionAsignadaInfo.java            (record aux)
│   └── DonacionEntregaInfo.java             (record aux)
├── entidadbeneficiaria/
│   ├── ObtenerTodasEntidadesResponse.java   (record)
│   ├── ObtenerEntidadResponse.java          (record)
│   ├── CrearEntidadBeneficiariaRequest.java (record - reemplaza existente)
│   ├── CrearEntidadBeneficiariaResponse.java (record)
│   ├── ActualizarEntidadBeneficiariaRequest.java (record - reemplaza existente)
│   ├── ActualizarEntidadBeneficiariaResponse.java (record)
│   ├── ObtenerNecesidadesResponse.java      (record)
│   ├── RegistrarNecesidadRequest.java       (record)
│   ├── RegistrarNecesidadResponse.java      (record)
│   ├── ReportarNoRecibidaRequest.java       (record)
│   ├── SubirFotosRecepcionRequest.java      (record)
│   └── NecesidadInfo.java                   (record aux)
├── evento/
│   ├── InicioRutaRequest.java               (record)
│   ├── InicioRutaResponse.java              (record)
│   ├── ConfirmacionEntregaExitosaRequest.java (record)
│   ├── ConfirmacionEntregaExitosaResponse.java (record)
│   └── ParadaInfo.java                      (record aux)
└── matchmaking/
    ├── ObtenerPropuestasPendientesResponse.java (record)
    ├── AceptarPropuestaResponse.java        (record)
    ├── RechazarPropuestaResponse.java       (record)
    ├── ForzarEjecucionMatchmakingResponse.java (record)
    └── PropuestaMatchmakingInfo.java        (record aux)

logistica/dto/
├── camion/
│   ├── ObtenerTodosCamionesResponse.java    (record)
│   ├── ObtenerCamionResponse.java           (record)
│   ├── CrearCamionRequest.java              (record)
│   ├── CrearCamionResponse.java             (record)
│   ├── ActualizarCamionRequest.java         (record)
│   ├── ActualizarCamionResponse.java        (record)
│   └── ChoferInfo.java                      (record aux)
├── entregadonaciones/
│   ├── IniciarRutaResponse.java             (record)
│   └── ConfirmarEntregaExitosaResponse.java (record)
├── monitoreo/
│   ├── RecibirTelemetriaRequest.java        (record)
│   └── ObtenerUbicacionResponse.java        (record)
├── planificacion/
│   ├── EjecutarPlanificacionRequest.java    (record)
│   ├── CamionPlanificacionInfo.java         (record aux)
│   └── DireccionInfo.java                   (record aux)
└── ruta/
    ├── ObtenerTodasRutasResponse.java       (record)
    ├── ObtenerRutaResponse.java             (record)
    ├── RutaInfo.java                        (record aux)
    └── ParadaInfo.java                      (record aux)
```

---

## Plan de verificación

### Compilación
- `mvn compile` en `donaciones/` sin errores
- `mvn compile` en `logistica/` sin errores

### Manual
- Verificar que todos los endpoints responden correctamente con los nuevos DTOs via Postman/Swagger
- Verificar que los services devuelven correctamente los tipos de response DTO esperados
