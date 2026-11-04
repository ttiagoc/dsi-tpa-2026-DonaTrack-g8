# Bocetos de interfaz

Todos los PNG están en el mismo formato: 1280 px de ancho, con el `styles.css` real y la fuente del sitio.
- **Pantallas nuevas:** primero una maqueta HTML con el `styles.css` real, exportada a PNG antes de implementarla.
- **Después de la revisión de diseño (navbar, menú lateral, footer, modales con cruz y unidad de medida):** todas las pantallas de A se volvieron a capturar de la aplicación levantada, con datos de ejemplo, para que boceto y código coincidan.

En [Figma](https://www.figma.com/design/1tzIHQUO6aSwCbdgtPuXgj) quedó una primera versión de las pantallas 1, 3, 4 y 10 que ya no está al día: los bocetos vigentes son estos PNG.

## Componentes de interfaz (común a todos los roles)

| Req | Pantalla | Ruta | Boceto |
|---|---|---|---|
| — | Biblioteca de componentes reutilizables (encabezado, filtros, listado, botones, estados) | — | [00-componentes.png](00-componentes.png) |

## Persona A: público y entidad beneficiaria

| Req | Pantalla | Ruta | Boceto |
|---|---|---|---|
| 1 | Landing | `/` | [01-landing.png](01-landing.png) |
| 1 | Términos y privacidad (sin sesión) | `/legal` | [01b-terminos.png](01b-terminos.png) |
| 1 | Términos y privacidad (con sesión: misma página, layout con menú lateral) | `/legal` | [01c-terminos-con-sesion.png](01c-terminos-con-sesion.png) |
| 2 | Mapa de entregas (mapa ilustrativo, con una entidad elegida) | `/mapa` | [02-mapa.png](02-mapa.png) |
| 2 | Mapa de entregas: sin entregas todavía | `/mapa` | [02b-mapa-vacio.png](02b-mapa-vacio.png) |
| 2 | Mapa de entregas con sesión (misma página, layout con menú lateral; link en la sección "Plataforma" del menú) | `/mapa` | [02c-mapa-con-sesion.png](02c-mapa-con-sesion.png) |
| 3 | Registro: elegir tipo de cuenta | `/registro` | [03-registro-selector.png](03-registro-selector.png) |
| 3 | Registro de persona | `/registro/persona-humana` | [03a-registro-persona-humana.png](03a-registro-persona-humana.png) |
| 3 | Registro de empresa u organización | `/registro/persona-juridica` | [03b-registro-persona-juridica.png](03b-registro-persona-juridica.png) |
| 3 | Registro de entidad beneficiaria | `/registro/entidad` | [03c-registro-entidad.png](03c-registro-entidad.png) |
| 4 | Ingresar | `/login` | [04-login.png](04-login.png) |
| 10 | Necesidades: listado | `/entidad/necesidades` | [10-necesidades.png](10-necesidades.png) |
| 10 | Necesidades: modal de alta con errores (cantidad + "¿En qué se mide?") | `/entidad/necesidades` | [10b-necesidades-alta.png](10b-necesidades-alta.png) |
| 10 | Necesidades: confirmar eliminación | `/entidad/necesidades` | [10c-necesidades-eliminar.png](10c-necesidades-eliminar.png) |
| 10 | Necesidades: lista vacía | `/entidad/necesidades` | [10d-necesidades-vacia.png](10d-necesidades-vacia.png) |
| — | Layout con sesión: menú del perfil abierto (vale para todos los roles) | — | [10e-menu-perfil.png](10e-menu-perfil.png) |
| 10 | Necesidades: selector de subcategoría con buscador | `/entidad/necesidades` | [10f-necesidades-buscador.png](10f-necesidades-buscador.png) |
| 10 | Necesidades: no se puede eliminar (tiene donaciones en camino) | `/entidad/necesidades` | [10g-necesidades-no-se-puede-eliminar.png](10g-necesidades-no-se-puede-eliminar.png) |
| 11 | Donaciones asignadas | `/entidad/donaciones` | [11-donaciones-asignadas.png](11-donaciones-asignadas.png) |
| 11 | Donaciones asignadas: lista vacía | `/entidad/donaciones` | [11b-donaciones-vacia.png](11b-donaciones-vacia.png) |
| 12 | Confirmar recepción con fotos | `/entidad/donaciones/{id}/recepcion` | [12-confirmar-recepcion.png](12-confirmar-recepcion.png) |
| 12 | Confirmar recepción: sin fotos (error) | `/entidad/donaciones/{id}/recepcion` | [12b-confirmar-recepcion-error.png](12b-confirmar-recepcion-error.png) |

## Persona C: administración

| Req | Pantalla | Ruta | Boceto |
|---|---|---|---|
| 15 | Recepción en depósito: registro de donación presencial | `/admin/recepcion` | [15-recepcion.png](15-recepcion.png) |
| 15 | Recepción en depósito: modal de alta rápida de donante | `/admin/recepcion` | [15b-recepcion-alta-donante.png](15b-recepcion-alta-donante.png) |
| 16 | Depósito: stock disponible y vencidas | `/admin/deposito` | [16-deposito.png](16-deposito.png) |
| 16 | Depósito: modal para marcar donación como vencida | `/admin/deposito` | [16b-deposito-vencer.png](16b-deposito-vencer.png) |
| 17 | Matchmaking: propuestas de asignación pendientes | `/admin/matchmaking` | [17-matchmaking.png](17-matchmaking.png) |
| 17 | Matchmaking: modal interactivo de selección de entidad sugerida | `/admin/matchmaking` | [17b-matchmaking-sugerencias.png](17b-matchmaking-sugerencias.png) |
| 18 | Camiones: listado de flota | `/admin/camiones` | [18-camiones.png](18-camiones.png) |
| 18 | Camiones: modal de alta y edición | `/admin/camiones` | [18b-camiones-alta.png](18b-camiones-alta.png) |
| 18 | Camiones: modal de confirmación de eliminación | `/admin/camiones` | [18c-camiones-eliminar.png](18c-camiones-eliminar.png) |
| 19 | Importación masiva: carga de donantes CSV (>10.000 filas) e historial | `/admin/importaciones` | [19-importaciones.png](19-importaciones.png) |
| 19 | Importación masiva: progreso en segundo plano y detalle de errores | `/admin/importaciones` | [19b-importaciones-progreso.png](19b-importaciones-progreso.png) |

Convención de nombres: `<req>[letra]-<pantalla>.png`. B y C agregan sus tablas (pantallas 5–9, 13–14 y 15–19) en este mismo archivo.
