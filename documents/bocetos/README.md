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
| 2 | Mapa de entregas (OpenStreetMap con Leaflet, cada entidad en su dirección, con una entidad elegida) | `/mapa` | [02-mapa.png](02-mapa.png) |
| 2 | Mapa de entregas: sin entregas todavía | `/mapa` | [02b-mapa-vacio.png](02b-mapa-vacio.png) |
| 2 | Mapa de entregas con sesión (misma página, layout con menú lateral; link en la sección "Plataforma" del menú) | `/mapa` | [02c-mapa-con-sesion.png](02c-mapa-con-sesion.png) |
| 3 | Registro: elegir tipo de cuenta | `/registro` | [03-registro-selector.png](03-registro-selector.png) |
| 3 | Registro de persona | `/registro/persona-humana` | [03a-registro-persona-humana.png](03a-registro-persona-humana.png) |
| 3 | Registro de empresa u organización | `/registro/persona-juridica` | [03b-registro-persona-juridica.png](03b-registro-persona-juridica.png) |
| 3 | Registro de entidad beneficiaria (dirección en campos separados, verificada en el mapa) | `/registro/entidad` | [03c-registro-entidad.png](03c-registro-entidad.png) |
| 3 | Registro de entidad beneficiaria: la dirección no existe | `/registro/entidad` | [03d-registro-entidad-direccion-no-encontrada.png](03d-registro-entidad-direccion-no-encontrada.png) |
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
| 15 | Categorías y subcategorías: catálogo que usa la recepción | `/admin/categorias` | [15c-categorias.png](15c-categorias.png) |
| 15 | Categorías: modal de alta con errores | `/admin/categorias?nueva` | [15d-categorias-alta.png](15d-categorias-alta.png) |
| 15 | Categorías: modal de edición de una subcategoría | `/admin/categorias?editarSub={id}` | [15e-categorias-subcategoria.png](15e-categorias-subcategoria.png) |
| 15 | Categorías: confirmar eliminación (borra también sus subcategorías) | `/admin/categorias?eliminar={id}` | [15f-categorias-eliminar.png](15f-categorias-eliminar.png) |
| 15 | Categorías: no se puede eliminar (la usan donaciones o necesidades) | `/admin/categorias?eliminar={id}` | [15g-categorias-no-se-puede-eliminar.png](15g-categorias-no-se-puede-eliminar.png) |
| 15 | Donantes: listado con buscador, contacto preferido y donaciones | `/admin/donantes` | [15h-donantes.png](15h-donantes.png) |
| 15 | Donantes: detalle con todos los datos | `/admin/donantes?ver={id}` | [15i-donantes-detalle.png](15i-donantes-detalle.png) |
| 15 | Donantes: modal de alta con errores | `/admin/donantes?nuevo` | [15j-donantes-alta.png](15j-donantes-alta.png) |
| 15 | Donantes: modal de edición (el CUIT/DNI no se cambia) | `/admin/donantes?editar={id}` | [15k-donantes-editar.png](15k-donantes-editar.png) |
| 15 | Donantes: confirmar eliminación | `/admin/donantes?eliminar={id}` | [15l-donantes-eliminar.png](15l-donantes-eliminar.png) |
| 15 | Donantes: no se puede eliminar (tiene donaciones) | `/admin/donantes?eliminar={id}` | [15m-donantes-no-se-puede-eliminar.png](15m-donantes-no-se-puede-eliminar.png) |
| 15 | Donantes: búsqueda sin resultados | `/admin/donantes?q=...` | [15n-donantes-sin-resultados.png](15n-donantes-sin-resultados.png) |
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
