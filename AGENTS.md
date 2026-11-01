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
2. **Exportarlo a PNG** en [`documents/bocetos/`](documents/bocetos/).
3. **Respetar el formato `punto-nombre.png`**:
   - `punto` es el número del requisito de la consigna, con dos dígitos: `04-login.png`, `12-confirmar-recepcion.png`.
   - Si un requisito tiene varias pantallas, se agrega una letra: `03a-registro-persona-humana.png`, `03b-registro-persona-juridica.png`.
   - `nombre` va en minúsculas, sin tildes y con guiones.
   - Agregar la fila correspondiente en [`documents/bocetos/README.md`](documents/bocetos/README.md) (requisito, pantalla, ruta y archivo).
4. **Implementarlo en código** siguiendo el boceto (controller, ruta con roles en `Router`, plantilla en `templates/<rol>/` y estilos de `styles.css`, como explica el README del `web-service`). Si al implementar hay que cambiar algo, se actualiza también el boceto y su PNG.

Al terminar, tildar el boceto y la pantalla en el plan que corresponda.
