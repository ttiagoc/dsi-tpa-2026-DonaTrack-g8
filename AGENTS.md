# AGENTS.md

Instrucciones para quien trabaje en este repo (personas o agentes de IA).

## Contexto
- Consigna de la entrega actual: [`documents/ConsignaTPA4.md`](documents/ConsignaTPA4.md).
- Plan de cada integrante: [`documents/planes-tpa4/`](documents/planes-tpa4/) (A: público y entidad, B: donante, C: admin y despliegue).
- Cómo agregar una pantalla al sitio web: [`web-service/README.md`](web-service/README.md).
- Rama de trabajo: `entrega-4`.
- **Commits:** los firma solo quien los hace. Los agentes de IA no se agregan como colaboradores: nada de `Co-Authored-By` ni de "Generated with ..." en commits ni en PRs.

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
  - **Menú lateral fijo** a la izquierda con las páginas del rol, agrupadas en secciones y con ícono. Cada rol tiene su bloque (A: entidad, B: donante, C: administrador). Al final, la sección común "Plataforma" (Mapa de entregas).
  - En la cabecera del menú, a la derecha del logo, el botón para achicarlo y dejar solo los íconos y el logo (el navegador recuerda la preferencia). Achicado, el botón para agrandarlo queda sobre el borde del menú.
  - **Barra superior** solo con el perfil (avatar, nombre y rol), que despliega un menú con "Salir" (y "Datos de ..." cuando exista esa pantalla). Sin título: el de cada página va en su `<h1>`.
  - **Footer** ("Términos y privacidad") al pie de la columna del contenido, el mismo que sin sesión.
  - En celular el menú lateral se oculta, se abre desde la barra y se cierra con la cruz de su cabecera. Sin sesión se usa el navbar superior: a la derecha las páginas públicas (Mapa de entregas) y, separados por una línea, "Ingresar" y "Registrarme".
- **ABM:** cada pantalla es un listado con el botón "+ Nuevo/a ..." arriba a la derecha. El alta y la edición se hacen en un modal (`<dialog>`). Si el servidor rechaza los datos, la página vuelve a cargarse con el modal abierto, los errores marcados y lo que se había escrito.
- **Los avisos (toasts de `partials/alertas`) tienen una cruz** a la derecha para cerrarlos.
- **Todo modal tiene una cruz para cerrarlo** arriba a la derecha (`.modal__cerrar` con `data-cierra` dentro de `.modal__encabezado`, ícono `#i-cerrar`) y `autofocus` en la acción segura (Cancelar, Entendido o el primer campo). Ejemplo en `templates/entidad/necesidades.html.hbs`.
- **Eliminar siempre pide confirmación** en un modal que nombra el registro y dice qué pasa al borrarlo. El borrado es un `POST` (los formularios HTML no mandan `DELETE`).
- **Lista vacía:** un mensaje que explica cómo cargar el primer registro, nunca una tabla vacía.
- **Validaciones en los dos lados:** cada formulario valida en el navegador (`public/js/formularios.js`, se activa solo con `novalidate`) y otra vez en el servidor (`Formulario`). Usar `tipo="email"` / `tipo="tel"` del partial `campo` y `form.email(...)` / `form.telefono(...)` en el controller, así las reglas y los mensajes son los mismos.
- **Selects con muchas opciones** (subcategorías, entidades, donantes): agregar `data-buscador="Buscá ..."` al `<select>` y se convierte en un campo con buscador (`public/js/selector-buscador.js`).
- **Campos que dependen entre sí:** si se pueden unir en uno solo, se unen (por ejemplo, tipo + período de una necesidad = "¿Cada cuánto la necesitan?").
