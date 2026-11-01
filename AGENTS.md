# AGENTS.md

Instrucciones para quien trabaje en este repo (personas o agentes de IA).

## Contexto
- Consigna de la entrega actual: [`documents/ConsignaTPA4.md`](documents/ConsignaTPA4.md).
- Plan de cada integrante: [`documents/planes-tpa4/`](documents/planes-tpa4/) (A: público y entidad, B: donante, C: admin y despliegue).
- Cómo agregar una pantalla al sitio web: [`web-service/README.md`](web-service/README.md).
- Rama de trabajo: `entrega-4`.

## Flujo para cada pantalla nueva

Primero el boceto y después el código. La consigna pide realizar los bocetos y después implementarlos, así que lo implementado tiene que coincidir con el boceto.

1. **Armar el boceto** con cualquier herramienta. En lo posible, usar Figma: en [DonaTrack - Bocetos](https://www.figma.com/design/1tzIHQUO6aSwCbdgtPuXgj) ya están los colores de `styles.css` como variables y los componentes `Botón`, `Campo`, `Navbar / Público` y `Footer`. Hacer un marco de 1280 px de ancho por pantalla y ponerle de nombre `<punto> · <Pantalla>  <ruta>` (por ejemplo, `4 · Ingresar  /login`).
   Alternativa (la que se usó en la pantalla 10, porque el plan gratuito de Figma limita las llamadas del MCP): una maqueta HTML con el `styles.css` real, exportada a PNG con Chrome o Edge en modo headless (`--headless --screenshot`). La maqueta no se versiona: en el repo queda solo el PNG.
2. **Exportarlo a PNG** en [`documents/bocetos/`](documents/bocetos/).
3. **Respetar el formato `punto-nombre.png`**:
   - `punto` es el número del requisito de la consigna, con dos dígitos: `04-login.png`, `12-confirmar-recepcion.png`.
   - Si un requisito tiene varias pantallas, se agrega una letra: `03a-registro-persona-humana.png`, `03b-registro-persona-juridica.png`.
   - `nombre` va en minúsculas, sin tildes y con guiones.
   - Agregar la fila correspondiente en [`documents/bocetos/README.md`](documents/bocetos/README.md) (requisito, pantalla, ruta y archivo).
4. **Implementarlo en código** siguiendo el boceto (controller, ruta con roles en `Router`, plantilla en `templates/<rol>/` y estilos de `styles.css`, como explica el README del `web-service`). Si al implementar hay que cambiar algo, se actualiza también el boceto y su PNG.

Al terminar, tildar el boceto y la pantalla en el plan que corresponda.

## Convenciones de pantallas
- **Navegación con sesión** (ver `documents/bocetos/10-necesidades.png` y `10e-menu-perfil.png`):
  - **Menú lateral fijo** a la izquierda con las páginas del rol, agrupadas en secciones y con ícono. Cada rol tiene su bloque (A: entidad, B: donante, C: administrador).
  - **Barra superior** con el nombre del panel, la campana de notificaciones con la cantidad sin leer y el perfil (avatar, nombre y rol), que despliega un menú con "Datos de ..." y "Salir".
  - En celular el menú lateral se oculta y se abre desde la barra. Sin sesión se usa el navbar superior de siempre.
- **ABM:** cada pantalla es un listado con el botón "+ Nuevo/a ..." arriba a la derecha. El alta y la edición se hacen en un modal (`<dialog>`). Si el servidor rechaza los datos, la página vuelve a cargarse con el modal abierto, los errores marcados y lo que se había escrito.
- **Eliminar siempre pide confirmación** en un modal que nombra el registro y dice qué pasa al borrarlo. El borrado es un `POST` (los formularios HTML no mandan `DELETE`).
- **Lista vacía:** un mensaje que explica cómo cargar el primer registro, nunca una tabla vacía.
- **Campos que dependen entre sí:** si se pueden unir en uno solo, se unen (por ejemplo, tipo + período de una necesidad = "¿Cada cuánto la necesitan?").
