# Errores a corregir

Hallazgos de código detectados de paso mientras se arma el diagrama ER (Entrega 3), que no son decisiones de persistencia sino bugs o inconsistencias del código/diagrama de clases existente. Se corrigen aparte, no bloquean el trabajo de persistencia.

## 1. `Donante.donaciones` / `agregarDonacion()` no se usan — `getFechaUltimaDonacion()` rompería en la práctica

- **Dónde**: [Donante.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/Donante.java)
- **Qué pasa**: `Donante.agregarDonacion()` no se invoca en ningún flujo real (`DonacionServiceImpl.crear()` nunca lo llama). `GestorDeEventos.verificarInactividadDonantes()` sí llama a `donante.getFechaUltimaDonacion()`, que calcula la fecha a partir de `this.donaciones` — lista que en la práctica siempre queda vacía. Con la implementación actual, `getFechaUltimaDonacion()` explota con `RuntimeException("El donante no tiene donaciones registradas.")` apenas se ejecute sobre un donante real.
- **La relación viva y real** es `Donacion.donante` (FK), no `Donante.donaciones`.
- **Sugerencia**: que `getFechaUltimaDonacion()` (o el servicio que la necesita) resuelva la fecha con una consulta sobre `Donacion` filtrando por `donante_id` y tomando el máximo `fecha`, en vez de depender de la lista. Ajustar también el diagrama de clases (entregable 1) para que `Donante` no muestre `donaciones : List<RegistroDonacion>` si esa asociación deja de existir.
- **Detectado en**: sesión de armado de [DiagramaER.puml](DiagramaER.puml), entidad `Donante`.

## 2. Discrepancias entre `DiagramaDeClases.puml` y el código real (entidades de `Donante`)

- **Dónde**: [DiagramaDeClases.puml](DiagramaDeClases.puml) vs. [PersonaJuridica.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/PersonaJuridica.java) / [PersonaHumana.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/PersonaHumana.java)
- **Qué pasa**: el diagrama de clases dice `PersonaJuridica.tipo : TipoOrganizacion` (un enum), pero en el código es `String tipo` — no existe ningún enum `TipoOrganizacion` en todo el repo. El diagrama también dice `PersonaHumana.fechaNacimiento : LocalDateTime`, pero en el código es `LocalDate`.
- **Sugerencia**: al actualizar el diagrama de clases para el entregable 1, decidir si conviene introducir el enum `TipoOrganizacion` (más prolijo) o dejar `String` y corregir el diagrama para que coincida con el código. Mientras tanto, el ER diagram (`DiagramaER.puml`) se mapeó siguiendo el código real.
- **Detectado en**: sesión de armado de `DiagramaER.puml`, entidad `Donante`.

## 3. `NecesidadRecurrente.periodo` nunca se completa desde la API — una necesidad recurrente nunca puede satisfacerse

- **Dónde**: [EntidadBeneficiariaServiceImpl.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/impl/EntidadBeneficiariaServiceImpl.java) (`toTipoNecesidad()`), [NecesidadRecurrente.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/entidades/NecesidadRecurrente.java)
- **Qué pasa**: `toTipoNecesidad("recurrente")` siempre construye `new NecesidadRecurrente()` sin argumentos — `NecesidadRequest` no tiene ningún campo para mandar el `periodo`. `NecesidadRecurrente.estaSatisfecha()` filtra las donaciones por `donacion.estaDentroDelPeriodoActual(this.periodo)`, que internamente hace `periodo != null && periodo.incluye(...)`. Con `periodo` siempre `null`, ese filtro siempre descarta todo, y la necesidad recurrente nunca se puede marcar como satisfecha (salvo que `cantidadRequerida` sea 0).
- **Sugerencia**: agregar `periodo` a `NecesidadRequest` (y a la validación en `EntidadBeneficiariaServiceImpl`) para que se pueda setear al crear una necesidad recurrente.
- **Detectado en**: sesión de armado de `DiagramaER.puml`, entidad `Necesidad`.

## 4. `Ruta.chofer` nunca se setea — campo muerto que duplica `Camion.chofer`

- **Dónde**: [Ruta.java](../logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/models/entities/logistica/Ruta.java), [RutaServiceImpl.java](../logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/services/impl/RutaServiceImpl.java)
- **Qué pasa**: `Ruta` tiene un campo `chofer` propio, pero no hay ningún setter invocado sobre él en todo `RutaServiceImpl` — ni al crear (`toRuta()`) ni al actualizar (`actualizar()`). El chofer efectivo de una ruta siempre se termina leyendo indirectamente vía `ruta.getCamion().getChofer()`. El campo queda siempre `null`.
- **Sugerencia**: si la intención es que el chofer de una ruta pueda diferir del chofer "actual" del camión (por ejemplo, para dejar registrado quién manejó ese viaje puntual aunque después cambie el chofer asignado al camión), hay que completar el campo al crear/asignar la ruta. Si no, conviene eliminarlo del modelo y depender siempre de `camion.chofer`, para no tener dos fuentes de verdad.
- **Detectado en**: sesión de armado de `DiagramaER.puml`, entidad `Ruta`.

## 5. `Parada` se direcciona por `orden`, no por un id propio — atención al portar la API a JPA

- **Dónde**: [RutaServiceImpl.java](../logistica-service/src/main/java/ar/edu/utn/frba/ddsi/logistica/services/impl/RutaServiceImpl.java) (`confirmarEntregaExitosa()`)
- **Qué pasa**: `POST /api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones` recibe un `paradaId`, pero el código busca la parada con `p.getOrden() == paradaId.intValue()` — es decir, `paradaId` en la URL es en realidad el `orden` de la parada dentro de esa ruta, no un id real (`Parada` no tiene `id` en el dominio). Con la persistencia, `Parada` va a pasar a tener un `id` real generado por la base (ver justificación en `JustificacionesDisenoRelacional.md`, sección `Parada`) — no es un bug a corregir ahora, pero al implementar el repositorio JPA conviene decidir si el endpoint sigue direccionando por `orden` (como hoy) o pasa a usar el nuevo `id` real, que sería más robusto (el `orden` podría repetirse o reordenarse).
- **Detectado en**: sesión de armado de `DiagramaER.puml`, entidad `Parada`.

## 6. `toCategoria()`/`toSubcategoria()` crean categorías nuevas al vuelo — pero las categorías se cargan de antes

- **Dónde**: [DonacionServiceImpl.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/impl/DonacionServiceImpl.java) y [EntidadBeneficiariaServiceImpl.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/impl/EntidadBeneficiariaServiceImpl.java) (`toCategoria()`/`toSubcategoria()`)
- **Qué pasa**: al introducir el catálogo real de `Categoria`/`Subcategoria` (ver `JustificacionesDisenoRelacional.md`), se implementó la lógica como "buscar por nombre, si no existe crearla" (`findByNombre(...).orElseGet(() -> repo.save(new ...))`). Pero según indicó el usuario, **las categorías se cargan de antes** (carga inicial / seed), no se crean sobre la marcha a partir de lo que mande un `Bien`/`Donacion`/`Necesidad`. Eso probablemente significa que el comportamiento correcto es más estricto: buscar por nombre y **fallar si no existe** (en vez de crearla silenciosamente), en vez del "find-or-create" actual.
- **Pendiente**: revisar con el usuario cómo se espera que se haga esa carga inicial (¿un endpoint de catálogo? ¿un seed/fixture al levantar el servicio?) y ajustar `toCategoria()`/`toSubcategoria()` en consecuencia.
- **Detectado en**: sesión de armado de `DiagramaER.puml`, decisión de `Categoria`/`Subcategoria`.
