# Correcciones pendientes · Entrega 4

Revisión de la app levantada con los datos de demostración (`.\run.ps1 seed`, usuarios en
[`docker/datos-demo/cargar-datos-demo.ps1`](../docker/datos-demo/cargar-datos-demo.ps1)). Cada punto tiene
**qué pasa** (con el archivo donde se ve), **propuesta** y **a quién le toca** según los
[planes](planes-tpa4/) (A: público y entidad, B: donante, C: admin y despliegue).

Recordatorio del [AGENTS.md](../AGENTS.md): si un cambio toca una pantalla, primero se actualiza el boceto
(PNG en [`bocetos/`](bocetos/)) y después el código.

## Resumen

| # | Tema | Prioridad | Afecta requisitos | Le toca a |
|---|---|---|---|---|
| 1 | Notificaciones sin destinatario: las bandejas siempre están vacías | **Alta** | 8, 13 | B (+ C en logística) |
| 2 | Botones y listados con formatos distintos: **componentes reutilizables** | **Alta** | todas las pantallas de listado | C arma los componentes; cada uno migra las suyas |
| 3 | Sin indicador de carga en los procesos largos | Media | 12, 15, 17, 19 | C (+ A) |
| 4 | Detalle de cada importación (qué se importó) | Baja (extra) | 19 | C |
| 5 | Filtros de "Mis donaciones" mal alineados y falta la subcategoría | **Alta** | 5 | B |
| 6 | "Entidades" sin estilos ni nombre (se rehace con los componentes del punto 2) | **Alta** | 6 | B |
| 7 | Incentivos: **sí** se pide (req. 7); solo hay que ocultarlo a las jurídicas | Baja | 7 | B |
| 8 | Mapa de entregas ilustrativo vs. mapa real de seguimiento | Media | 2 | A |
| 9 | Requisitos incompletos (importación sin usuario, alta de jurídica, etc.) | **Alta** | 5, 15, 19 | B, C |
| — | Otros hallazgos (datos fijos en el código, permisos, bocetos de B) | Media | — | ver abajo |

---

## 1. Notificaciones sin destinatario

**Qué pasa.** El seed genera 47 notificaciones (asignación, en camino, entrega, misión cumplida, entrega
fallida, alta por importación), pero en la web las bandejas de donante y entidad aparecen vacías.

- La web pide `GET /api/notificaciones?destinatarioId={perfilId}&tipoDestinatario=DONANTE|ENTIDAD`
  ([DonanteWebController.java:76](../web-service/src/main/java/ar/edu/utn/frba/ddsi/web/controllers/DonanteWebController.java#L76) y `:88`).
- `notificaciones-service` guarda esos campos ([Notificacion.java](../notificaciones-service/src/main/java/ar/edu/utn/frba/ddsi/notificaciones/models/entities/Notificacion.java)), pero nadie se los manda.
  El `NotificacionRequest` de `donaciones-service` y de `logistica-service` solo tiene `valor`, `tipoContacto` y `mensaje`
  ([donaciones · NotificacionRequest.java](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/evento/NotificacionRequest.java)).
  En la base todas quedan con `tipo_destinatario`, `destinatario_id`, `tipo_evento` y `donacion_id` en `NULL`.
- Además, **confirmar la recepción no notifica a nadie** ([DonacionServiceImpl.confirmarRecepcion](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/impl/DonacionServiceImpl.java#L197)).
  El req. 8 pide avisarle al donante "cuando la entidad beneficiaria asignada recibe la donación" y el 13, a la entidad, sobre la confirmación de recepción.
- Los mensajes son genéricos: "Tu donación ha sido asignada." no dice cuál ni a quién.
- `GET /api/notificaciones` no tiene roles ([NotificacionController.java:33](../notificaciones-service/src/main/java/ar/edu/utn/frba/ddsi/notificaciones/controllers/NotificacionController.java#L33)): cualquiera puede leer las notificaciones de otro.

**Propuesta.**
1. Completar el DTO en los dos servicios que envían, con los mismos campos que espera `notificaciones-service`:
   `tipoEvento`, `tipoDestinatario` (`DONANTE` / `ENTIDAD`), `destinatarioId` (id del perfil, no del usuario) y `donacionId`.
   Mejor aún: moverlo a `common-lib` para que no se vuelva a desincronizar.
2. Completar los datos en cada lugar que arma una notificación:

   | Dónde | `tipoEvento` | Destinatarios |
   |---|---|---|
   | `DonacionAsignadaListener` | `DONACION_ASIGNADA` | donante + entidad |
   | `InicioRutaListener` (logística) | `EN_CAMINO` | donante + entidad de cada parada |
   | `EntregaExitosaListener` | `ENTREGA_EXITOSA` | donante + entidad |
   | `EntregaFallidaListener` | `ENTREGA_FALLIDA` | donante + entidad |
   | `GamificacionServiceImpl` | `MISION_CUMPLIDA` / `SUBIDA_CATEGORIA` (ya los pone en el mensaje) | donante |
   | **nuevo** evento en `confirmarRecepcion` | `RECEPCION_CONFIRMADA` | donante (y entidad como comprobante) |
   | `ImportadorDeDonantes` | `ALTA_IMPORTACION` | donante creado |
   | `AusenciaPlataformaListener` | `AUSENCIA` | donante |
3. Mensajes con contexto: "Tu donación #5 (Jabón y shampoo) fue asignada a Hogar de Ancianos Santa Ana".
4. Proteger `GET /api/notificaciones` con roles (`DONANTE`, `ENTIDAD_BENEFICIARIA`, `ADMINISTRADOR`) y
   `Autorizacion.exigirPropietario(...)` con el `destinatarioId`, igual que en `/api/donaciones`.
5. Test: un test de integración por listener que verifique el `NotificacionRequest` enviado (con `MockRestServiceServer`).

**Cómo verificarlo:** `.\run.ps1 seed`, entrar como `ana` y `comedor` y ver las bandejas con avisos.

---

## 2. Componentes reutilizables para listados y botones

> Este punto junta lo que antes eran dos (botones con distinto formato y formato único de listado). La causa es la misma:
> cada pantalla arma a mano su encabezado, su tabla, sus filtros y sus botones, eligiendo las clases CSS cada vez.
> La solución también es una sola: **componentes que todas las pantallas usen**, de modo que nadie tenga que decidir de nuevo
> qué clase lleva un botón o cómo se arma una tabla.

### Qué pasa

**Botones.**

| Pantalla | Acción principal | Cómo se ve hoy |
|---|---|---|
| Camiones | `+ Nuevo camión` | primario, a la derecha del título ([camiones.html.hbs:8](../web-service/src/main/resources/templates/admin/camiones.html.hbs#L8)) |
| Necesidades | `+ Nueva necesidad` | primario, a la derecha del título |
| Recepción | `+ Nuevo donante` | **secundario, debajo del título** ([recepcion.html.hbs:8](../web-service/src/main/resources/templates/admin/recepcion.html.hbs#L8)) |
| Matchmaking | `Ejecutar matchmaking` | primario, **debajo del título** ([matchmaking.html.hbs:9](../web-service/src/main/resources/templates/admin/matchmaking.html.hbs#L9)) |
| Importaciones | `Comenzar importación` | usa `boton--primario`, una clase que **no existe** en `styles.css` |

En las filas de las tablas:
- Camiones: `Editar` y `Eliminar` aparecen **uno encima del otro**.
- Matchmaking: `Ver sugerencias` es un botón **relleno** (primario), mientras que en el resto las acciones de fila son de contorno.
- Depósito: `Marcar vencida` es de contorno rojo, como `Eliminar`, pero sin ícono.

**Listados.**
- [Entidades](../web-service/src/main/resources/templates/donante/entidades.html.hbs) usa tarjetas con clases que no existen (punto 6).
  Las demás pantallas usan tablas, pero cada una con su propia estructura: la última columna se llama "Acciones" en unas y no tiene
  nombre en otras, el resumen aparece o no, y el mensaje de lista vacía está escrito distinto en cada pantalla.
- Los filtros de [Mis donaciones](../web-service/src/main/resources/templates/donante/mis-donaciones.html.hbs) están armados con una
  `grilla--3` y quedan desalineados (punto 5). Depósito usa pestañas como filtro. No hay un componente de filtros.
- El estado de una donación se muestra de tres maneras distintas: crudo (`EN_DEPOSITO`), con texto legible ("Entregada") o con
  etiquetas de colores distintos según la pantalla. Las fechas, igual: ISO, `04/11/2026 19:08` o "El 04/11".
- El HTML de cada listado tiene **estilos en línea** (`style="..."`) repetidos, sobre todo en importaciones y recepción.

### Propuesta: una biblioteca chica de componentes

El proyecto ya tiene un componente que funciona así: [`partials/campo`](../web-service/src/main/resources/templates/partials/campo.html.hbs),
que todos los formularios usan para sus campos. La idea es hacer lo mismo con el resto de las piezas que se repiten.

Hay dos tipos de componentes:
- **Partials de Handlebars** para las piezas con estructura (encabezado, filtros, listado, modal). Con *partial blocks*
  (`{{#> partials/listado}} … {{/partials/listado}}` y, adentro del partial, `{{> @partial-block}}`) la pantalla pone su
  contenido dentro del componente. Handlebars.java 4.3.1, la versión del proyecto, los soporta; **conviene validarlo primero con
  una prueba chica**. Si no anduvieran, se usa el mismo mecanismo de `{{#partial}}` que ya usa `layouts/base`.
- **Helpers en Java** para lo que sale de mapear un valor a un formato (botones, estados, fechas). La regla queda en un solo
  lugar, y además se puede testear con JUnit.

#### Los componentes

| Componente | Tipo | Qué resuelve | Uso en una pantalla |
|---|---|---|---|
| `encabezado` | partial | `<h1>`, bajada y **una** acción de página a la derecha | `{{> partials/encabezado titulo="Camiones" bajada="Flota disponible…" accion=accionAlta}}` |
| `boton` | helper | Todas las variantes de botón, elegidas por **qué hace** y no por clase CSS | `{{boton "editar" href=urlEditar sr=patente}}` |
| `filtros` | partial block | Formulario `GET` con los campos en fila y "Filtrar" / "Limpiar" alineados al final | `{{#> partials/filtros limpiar="/donante/donaciones"}} …campos… {{/partials/filtros}}` |
| `listado` | partial block | Tarjeta + tabla con `caption`, o el mensaje de lista vacía si no hay filas | `{{#> partials/listado items=camiones caption="Camiones de la flota" vacio=vacio}} …thead y tbody… {{/partials/listado}}` |
| `acciones-fila` | partial block | Celda "Acciones" con los botones en una sola fila, alineados a la derecha | `{{#> partials/acciones-fila}} {{boton "editar" …}} {{boton "eliminar" …}} {{/partials/acciones-fila}}` |
| `estado` | helper | Etiqueta de estado de donación: texto legible y color fijo para cada estado | `{{estado estadoActual}}` → `<span class="etiqueta etiqueta--ok">Entregada</span>` |
| `fecha` | helper | Un solo formato de fecha (`dd/MM/yyyy`, y con `hora=true` agrega `HH:mm`) | `{{fecha fechaEstado}}` |
| `modal-confirmar` | partial | El modal de "¿Seguro que querés…?" de eliminar, vencer o descartar | `{{> partials/modal-confirmar id="modal-eliminar" titulo=… mensaje=… accion=… boton="Eliminar camión"}}` |
| `resumen` | (CSS que ya existe) | Indicadores arriba del listado; siempre con datos **reales** | Se mantiene `.resumen` / `.resumen__item` |
| `paginacion` | partial | "Anterior / Página 2 de 7 / Siguiente" al pie del listado (lo necesita el punto 4) | `{{> partials/paginacion pagina=pagina total=paginas base=urlBase}}` |

#### El helper `boton`: las pantallas eligen el *tipo*, no la clase

Cada tipo tiene una variante, un tamaño y un ícono fijos. Así es imposible que una pantalla ponga un "Ver" relleno y otra uno de contorno.

| Tipo | Dónde se usa | Clases | Ícono |
|---|---|---|---|
| `alta` | Encabezado: "+ Nuevo camión", "+ Nueva necesidad", "+ Nuevo donante" | `boton` | `i-mas` (nuevo) |
| `pagina` | Encabezado: "Ejecutar matchmaking", "Comenzar importación" | `boton` | el que se pase |
| `ver` | Fila: "Ver sugerencias", "Ver detalle", "Confirmar recepción" | `boton boton--secundario boton--chico` | `i-ojo` (nuevo) |
| `editar` | Fila | `boton boton--secundario boton--chico` | `i-lapiz` (nuevo) |
| `eliminar` | Fila: "Eliminar", "Marcar vencida", "Descartar" | `boton boton--secundario boton--peligro boton--chico` | `i-basura` (nuevo) |
| `confirmar` | Pie de modal o formulario: "Guardar", "Confirmar asignación" | `boton` | — |
| `confirmar-peligro` | Pie del modal de confirmación: "Eliminar camión" | `boton boton--peligro` | — |
| `cancelar` | Pie de modal: "Cancelar" (con `data-cierra` y `autofocus`) | `boton boton--secundario` | — |

Parámetros comunes: `texto`, `href` (si está, genera un `<a>`; si no, un `<button>`), `modal` (genera `data-abre`), `sr` (el texto
`sr-only` que dice sobre qué registro actúa: "Editar <span class="sr-only">camión AE123BC</span>"), `formaction` y `cargando` (el texto
del punto 3: con eso cada botón ya trae su indicador de carga).

Boceto de la implementación:

```java
// web-service/.../server/templates/TipoBoton.java
enum TipoBoton {
    ALTA("boton", "i-mas"), PAGINA("boton", null),
    VER("boton boton--secundario boton--chico", "i-ojo"),
    EDITAR("boton boton--secundario boton--chico", "i-lapiz"),
    ELIMINAR("boton boton--secundario boton--peligro boton--chico", "i-basura"),
    CONFIRMAR("boton", null), CONFIRMAR_PELIGRO("boton boton--peligro", null),
    CANCELAR("boton boton--secundario", null);
    …
}

// en JavalinHandlebars, junto a ConditionalHelpers
handlebars.registerHelper("boton", new BotonHelper());   // arma <a>/<button> con las clases, el ícono y el sr-only
handlebars.registerHelper("estado", new EstadoHelper()); // reutiliza SituacionDonacion
handlebars.registerHelper("fecha", new FechaHelper());
```

#### Cómo queda una pantalla (camiones, antes de unas 60 líneas de HTML)

```handlebars
{{#partial "contenido"}}
{{> partials/encabezado titulo="Camiones" bajada="Flota disponible para la logística de recolección y entrega de donaciones."
                        accion=accionAlta}}
<div class="resumen">…</div>

{{#> partials/listado items=camiones caption="Camiones de la flota" vacio=vacio}}
  <thead><tr><th scope="col">Patente</th><th scope="col">Chofer</th><th scope="col">Carga</th>
             <th scope="col">Volumen</th><th scope="col">Acciones</th></tr></thead>
  <tbody>
    {{#each camiones}}
      <tr>
        <td><strong>{{patente}}</strong></td><td>{{chofer}}</td><td>{{carga}} kg</td><td>{{volumen}} m³</td>
        {{#> partials/acciones-fila}}
          {{boton "editar" href=(concat "/admin/camiones?editar=" id) sr=(concat "camión " patente)}}
          {{boton "eliminar" href=(concat "/admin/camiones?eliminar=" id) sr=(concat "camión " patente)}}
        {{/partials/acciones-fila}}
      </tr>
    {{/each}}
  </tbody>
{{/partials/listado}}

{{> partials/modal-confirmar id="modal-eliminar" …}}
{{/partial}}
```

`accionAlta` y `vacio` los arma el controller (texto, link y modal), igual que hoy arma `resumen`. `concat` es un helper de una
línea o, más simple, el controller ya manda `urlEditar` y `etiqueta` en cada fila.

#### CSS

Agregar a `styles.css` una sección "Componentes de listado", **sin estilos en línea** en las plantillas:
- `.encabezado-pagina__accion`, para que la acción quede a la derecha en escritorio y abajo del título en celular.
- `.filtros`: `display:flex; flex-wrap:wrap; gap; align-items:flex-end`. Los `.campo` de adentro con `flex:1 1 12rem` y sin margen,
  y `.filtros__acciones` con los botones a la misma altura que los inputs. En celular, en columna.
- `.tabla__acciones`: `display:flex; gap:var(--espacio-2); justify-content:flex-end; flex-wrap:nowrap` (reemplaza a `.tabla__accion`).
- Un color de etiqueta por estado de donación (`--ok`, `--info`, `--pendiente`, `--error`, `--neutra`; hoy falta `--ok`).
- `.paginacion`.
- Sacar `boton--primario` (no existe) y las clases sueltas de Entidades.

#### Reglas para el AGENTS.md (sección "Convenciones de pantallas")

- Toda pantalla que lista datos usa `encabezado` + (`resumen`) + (`filtros`) + `listado`. **Nada de tablas ni tarjetas armadas a mano.**
- Los botones se escriben con `{{boton "<tipo>" …}}`; ninguna plantilla escribe `class="boton…"` a mano. Se puede chequear con un
  `grep` en el CI o en un test: si aparece `class="boton` en `templates/` fuera de `partials/`, falla.
- Una sola acción de página por pantalla, a la derecha del título. Las acciones de fila, todas de contorno y en la última columna, "Acciones".
- Estados y fechas, siempre con `{{estado}}` y `{{fecha}}`.
- Los filtros van por `GET` (la URL se puede compartir) y "Limpiar" es un link a la ruta sin parámetros.
- Lista vacía: el mensaje dice cómo cargar el primer registro. Si hay filtros aplicados: "Ninguna coincide con los filtros" + Limpiar.

#### Documentarlo y bocetarlo

- En [`web-service/README.md`](../web-service/README.md), una sección "Componentes" con un ejemplo de cada uno, como la que ya tiene `campo`.
- Un boceto `00-componentes.png` con todos los componentes y sus variantes (encabezado, filtros, listado lleno y vacío, cada tipo de
  botón, estados y paginación). Sirve de referencia para los demás bocetos y para la defensa ("¿cómo garantizan la consistencia?").
  Opcional: una página `/componentes`, solo en desarrollo, que los muestre todos y de la que se saque ese PNG.

### Cómo repartirlo

1. **Primero, una sola persona** (propuesta: C, que tiene más pantallas de listado) hace los componentes, los helpers con sus tests, el
   CSS, el boceto `00` y migra **una** pantalla de ejemplo: Camiones.
2. **Después, cada uno migra las suyas en paralelo**, sin tocar los componentes (si a alguien le falta algo, lo pide):

   | Persona | Pantallas |
   |---|---|
   | A | Necesidades, Donaciones asignadas, Notificaciones de la entidad |
   | B | Mis donaciones (con el punto 5), Entidades (punto 6), Incentivos, Notificaciones del donante |
   | C | Depósito, Matchmaking, Recepción (`+ Nuevo donante` pasa al encabezado), Importaciones (con el punto 4) |
3. Cada uno vuelve a capturar los bocetos de sus pantallas. Los cambios son visuales y no de flujo, así que alcanza con reemplazar los PNG.

---

## 3. Indicadores de carga

**Qué pasa.** Ningún formulario da señales de que está trabajando. [`formularios.js`](../web-service/src/main/resources/public/js/formularios.js)
solo valida. Se nota sobre todo en:
- **Importar CSV** (archivos de más de 10.000 filas, hasta 50 MB): mientras se sube el archivo la página queda quieta.
  El progreso aparece recién cuando el servidor contesta.
- **Ejecutar matchmaking** y **Confirmar asignación**.
- **Registrar recepción en depósito** y **Confirmar recepción** (sube hasta 5 fotos).
- **Ingresar** y **Registro** (pasan por la API de auth).

Además se puede hacer doble clic y mandar el formulario dos veces (por ejemplo, dos donaciones iguales).

**Propuesta.**
1. Un script chico, `public/js/cargando.js`, que se cargue en `layouts/base`:
   - Escucha `submit` en los `<form data-cargando="Importando archivo…">`.
   - Si `formularios.js` no canceló el envío (`!evento.defaultPrevented`): deshabilita los botones `type=submit`, cambia el texto por
     el de `data-cargando`, agrega la clase `boton--cargando` y pone `aria-busy="true"` en el form.
   - Con `pageshow` (volver atrás en el navegador) restaura el botón.
2. CSS: `.boton--cargando` con un spinner (`::before` + `@keyframes girar`). Con `prefers-reduced-motion`, sin animación.
   Con el helper `boton` del punto 2 alcanza con agregar `cargando="Importando archivo…"` al botón; el helper le pone al form el
   `data-cargando`, así ninguna pantalla lo tiene que acordar.
3. Importación: mostrar el avance de la **subida** con un `XMLHttpRequest` y su `upload.onprogress`
   (barra "Subiendo archivo… 40 %"). Después sigue con el progreso de procesamiento que ya existe (polling a `/admin/importaciones/{id}/estado`).
4. Seguimiento: mientras se pide la ubicación, un estado de "Buscando camiones…" en lugar del mapa vacío.
5. Boceto nuevo: `19c-importaciones-subiendo.png`, y una variante con botón en carga en algún boceto de formulario (por ejemplo `15c`).

---

## 4. Ver el detalle de cada importación (extra)

**Qué pasa.** No está pedido: el req. 19 solo pide importar. Pero hoy del historial no se puede saber **qué donantes** entraron.
- La fila del historial es un link escondido (`?activa=id`) que abre la tarjeta de progreso, y esa tarjeta solo muestra los errores.
- `ImportacionTarea` guarda contadores y errores, no los donantes creados o actualizados.
- Las tareas se guardan **en memoria** (`ConcurrentHashMap` en [ImportadorDeDonantes.java:44](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/ImportadorDeDonantes.java#L44)): se pierden al reiniciar el servicio.

**Propuesta.**
1. **Persistir** la importación y su resultado por fila: tabla `importacion` (lo que hoy es `ImportacionTarea`) y tabla
   `importacion_fila` (`importacion_id`, `fila`, `documento`, `nombre`, `email`, `resultado` CREADO/ACTUALIZADO/DESCARTADO, `motivo`, `donante_id`).
   Con 10.000+ filas, insertar por lotes (`hibernate.jdbc.batch_size`).
2. API: `GET /api/donantes/importaciones/{id}/filas?resultado=CREADO&pagina=1&tamanio=50` (paginado, solo `ADMINISTRADOR`).
3. Web: en el historial, una columna "Acciones" con `{{boton "ver" texto="Ver detalle"}}` (punto 2). Abre un `<dialog>` con:
   - el resumen arriba (creados / actualizados / descartados);
   - pestañas o un select por resultado;
   - una tabla paginada (fila, documento, nombre, email, motivo), con los componentes `listado` y `paginacion` del punto 2;
   - opcional: "Descargar errores (.csv)" para corregir y volver a subir.
   Igual que los demás modales: con `?detalle={id}` se abre al cargar la página, tiene la cruz y `autofocus` en "Cerrar".
4. Boceto `19c-importaciones-detalle.png`.

---

## 5. Filtros de "Mis donaciones"

**Qué pasa** ([mis-donaciones.html.hbs](../web-service/src/main/resources/templates/donante/mis-donaciones.html.hbs)):
- Los filtros están en una `grilla--3`. `Filtrar` y `Limpiar` caen en la tercera columna, más altos que los selects y sin alinearse
  con ellos (el `.campo` tiene margen abajo y los botones no).
- **Falta el filtro por subcategoría**, y el req. 5 dice "por estado y categoría **o subcategoría/s**".
  La API ya lo soporta (`subcategoriaIds` en `GET /api/donaciones`).
- El estado se ve crudo (`EN_DEPOSITO`, todos con la misma etiqueta azul) y la fecha también (`2026-11-04T19:08:58.679725`).
  En "Donaciones asignadas" de la entidad ya se muestran bien ("Entregada", "El 04/11").

**Propuesta.**
1. Armar la pantalla con los componentes del **punto 2**: `filtros` (resuelve la alineación), `listado` y `acciones-fila`.
2. Agregar "Subcategorías" como `<select multiple data-buscador="Buscá una subcategoría">`, que dependa de la categoría elegida.
3. Estado y fecha con los helpers `{{estado}}` y `{{fecha}}` del punto 2: texto legible, color fijo para cada estado y `dd/MM/yyyy`.
4. Boceto: `05-mis-donaciones.png` (hoy no existe, ver "Otros hallazgos").

---

## 6. Pantalla "Entidades"

**Qué pasa** ([donante/entidades.html.hbs](../web-service/src/main/resources/templates/donante/entidades.html.hbs)):
- Usa clases que **no existen** en `styles.css` (`panel`, `card`, `card--borde`, `grid--3`, `badge`, `texto--apagado`), así que sale sin estilos.
- Muestra `{{this.nombre}}` y `{{this.tipo}}`, pero la API devuelve `razonSocial`: **no aparece el nombre de ninguna entidad**, solo "Dirección: …".
- Es la única pantalla de listado con tarjetas en vez de tabla.

**Propuesta.** Rehacerla con los componentes del **punto 2**, como cualquier otro listado:
- `encabezado` sin acción (el donante no da de alta entidades).
- `filtros`: buscar por nombre y por categoría de necesidad.
- `listado` con las columnas Entidad (razón social, con la dirección debajo en `.tabla__secundario`), Necesidades vigentes (las 2 o 3
  primeras como etiquetas y "+N"), Donaciones recibidas y Acciones, con `{{boton "ver" texto="Ver necesidades"}}`, que abre un modal con todas.
- Corregir el modelo: el controller tiene que mandar `razonSocial` (o mapearlo a `nombre`) y sacar `tipo`, que la entidad no tiene.
- Antes del código: **boceto `06-entidades.png`**, hecho con el boceto de componentes (`00-componentes.png`).

---

## 7. Incentivos

La consigna **sí** lo pide. El **requisito 7** dice que la persona humana donante debe poder acceder a una sección de Incentivos y
consultar misiones a realizar e insignias obtenidas. Y el req. 8 pide notificar cuando cumple una misión o sube de categoría.
**No hay que sacarlo.** Lo que sí hay que corregir:
- Aplica **solo a personas humanas**, pero el menú lo muestra a todos los donantes
  ([menu-lateral.html.hbs:41](../web-service/src/main/resources/templates/partials/menu-lateral.html.hbs#L41)).
  Ocultarlo a las jurídicas (la sesión ya sabe el tipo de donante, o se agrega al `UsuarioSesion`) y devolver 404 si entran por URL.
- Que la pantalla use los componentes del punto 2 (`encabezado` y `listado` para las misiones).
- Boceto `07-incentivos.png`.

---

## 8. Mapa de entregas vs. mapa de seguimiento

**Qué pasa.**
- **Seguimiento** (req. 9 y 14) usa Leaflet + OpenStreetMap, un mapa real con zoom y marcadores.
- **Mapa de entregas** (req. 2) es un SVG ilustrativo: cada entidad cae en un punto calculado a partir de su id
  ([PosicionesEnMapa.java](../web-service/src/main/java/ar/edu/utn/frba/ddsi/web/controllers/PosicionesEnMapa.java)), así que no está en su
  dirección real. El comentario del código dice que los profes permitieron mockearlo.
- El req. 2 pide "un **mapa interactivo** y permitir ver sus detalles al hacer clic en cada marcador". Hoy se cumple, pero dos mapas
  distintos en la misma app no quedan bien.

**Propuesta: unificar en Leaflet.**
1. Coordenadas de la entidad: guardar `latitud` y `longitud` en `EntidadBeneficiaria` y completarlas al registrarla, geocodificando la
   dirección con la **API Georef** del Estado (`apis.datos.gob.ar/georef/api/direcciones`, gratis, pensada para direcciones argentinas).
   Si no encuentra la dirección, quedan nulas y la entidad no se dibuja (o se pide confirmar el punto en el registro).
2. `GET /api/donaciones/entregadas` devuelve también las coordenadas.
3. Un `public/js/mapa.js` compartido (crear el mapa, tiles, marcador, popup) que usen las tres pantallas: mapa público, seguimiento
   del donante y seguimiento de la entidad. Igual que hoy, el popup o el panel lateral muestra las entregas con sus fotos.
4. Borrar `PosicionesEnMapa` y el SVG, o dejarlos solo de respaldo si no hay red para los tiles.
5. Volver a capturar los bocetos 02, 02b y 02c.

Si el equipo prefiere no tocar el dominio antes de la entrega, la alternativa mínima es usar Leaflet con las posiciones calculadas
alrededor del centro de CABA. Así los dos mapas al menos se ven iguales.

---

## 9. Requisitos incompletos

Revisión contra la [consigna](ConsignaTPA4.md):

| Req | Qué falta | Propuesta |
|---|---|---|
| **5** | Filtro por subcategoría | Punto 5 |
| **8 / 13** | Las notificaciones no llegan a la web y no hay aviso de recepción confirmada | Punto 1 |
| **15** | El admin solo puede dar de alta **personas humanas** desde Recepción (`/admin/recepcion/donante-humana`); no hay alta de persona jurídica | Agregar al modal una opción "Persona / Organización" con los campos de jurídica. La API ya existe (`POST /api/donantes/persona-juridica`) |
| **19** | La importación **no otorga usuario**. El req. dice "importar personas donantes ya registradas **para otorgarles un usuario**"; hoy solo crea o actualiza el `Donante` | Por cada donante importado sin usuario, crear un `Usuario` (username = email) con una contraseña temporal o, mejor, un link de "elegí tu contraseña" con un token de un solo uso, enviado por mail (`ALTA_IMPORTACION`). En el detalle (punto 4), mostrar si se creó el usuario |
| **2** | Mapa ilustrativo | Punto 8 (opcional según lo que acepte el ayudante) |

**Sobre "registrar donaciones y seguir su estado":**
- **Registrar donaciones**: la consigna se lo pide al **administrador** (req. 15: "registrar personas donantes y donaciones que haya
  recibido en el depósito"), y eso está en `/admin/recepcion`. No pide que el donante cargue donaciones desde la web, aunque la API lo permite
  (`POST /api/donaciones` acepta el rol `DONANTE`). Si el equipo lo quiere como extra: "+ Nueva donación" en "Mis donaciones",
  reutilizando el formulario de bienes de Recepción, con el donante fijo en el de la sesión.
- **Seguir su estado**: hoy el donante ve el estado actual en la tabla (req. 5) y el camión en Seguimiento (req. 9). Lo que falta es ver
  **el historial** de una donación. Propuesta: `Ver detalle` en cada fila de "Mis donaciones", que abra un modal con la línea de tiempo de
  `CambioEstado` (fecha, estado y justificación: "Asignada a Comedor Los Pibes", "En camino · AF456DE", "Entregada", "Recepción confirmada
  con 2 fotos"). El historial ya se guarda; solo hay que exponerlo en el `DonacionResponse` o en `GET /api/donaciones/{id}/historial`.

---

## Otros hallazgos

1. **Faltan los bocetos de B** (req. 5 a 9, 13 y 14). El [README de bocetos](bocetos/README.md) tiene solo las tablas de A y C, y la consigna
   pide bocetar e implementar. Le toca a B: `05-mis-donaciones`, `06-entidades`, `07-incentivos`, `08-notificaciones`, `09-seguimiento`,
   `13-notificaciones-entidad`, `14-seguimiento-entidad`.
2. **Datos fijos en el código** que se ven como reales:
   - Matchmaking: "12 entidades candidatas" y "85 % compatibilidad promedio" son constantes
     ([MatchmakingAdminController.java:31](../web-service/src/main/java/ar/edu/utn/frba/ddsi/web/controllers/MatchmakingAdminController.java#L31)).
     Calcularlos o sacarlos.
   - Importaciones: arranca con 3 importaciones de ejemplo (IMP-102 a IMP-104, "32.805 donantes procesados") que no existieron
     ([ImportadorDeDonantes.java:57](../donaciones-service/src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donantes/ImportadorDeDonantes.java#L57)).
     Sacarlas: para la demo están los datos del seed.
3. **Credenciales reales en el repo local**: `notificaciones-service/src/main/resources/application.yaml` tiene una clave real de Resend.
   Está en `.gitignore` y nunca se subió (revisado en el historial de git). Igual conviene que cada uno use la suya o la de ejemplo.
4. Títulos de pestaña inconsistentes: "Catálogo de Entidades **—** DonaTrack" frente a "Camiones **·** DonaTrack". Unificar con `·`.
