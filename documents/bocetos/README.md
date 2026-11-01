# Bocetos de interfaz

Fuente editable en Figma: [DonaTrack - Bocetos](https://www.figma.com/design/1tzIHQUO6aSwCbdgtPuXgj). Los PNG de esta carpeta son exportaciones de ese archivo.

El archivo usa los colores de `web-service/.../public/css/styles.css` como variables y tiene una sección "Componentes" (`Botón`, `Campo`, `Navbar / Público`, `Footer`) para reutilizar en las pantallas nuevas. Tipografía: Roboto (de la pila de `--fuente`; Segoe UI no está en Figma).

## Persona A: público y entidad beneficiaria

| Req | Pantalla | Ruta | Boceto |
|---|---|---|---|
| 1 | Landing | `/` | [01-landing.png](01-landing.png) |
| 1 | Términos y privacidad | `/legal` | [01b-terminos.png](01b-terminos.png) |
| 2 | Mapa de donaciones entregadas | `/mapa` | Pendiente |
| 3 | Registro: elegir tipo de cuenta | `/registro` | [03-registro-selector.png](03-registro-selector.png) |
| 3 | Registro de persona | `/registro/persona-humana` | [03a-registro-persona-humana.png](03a-registro-persona-humana.png) |
| 3 | Registro de empresa u organización | `/registro/persona-juridica` | [03b-registro-persona-juridica.png](03b-registro-persona-juridica.png) |
| 3 | Registro de entidad beneficiaria | `/registro/entidad` | [03c-registro-entidad.png](03c-registro-entidad.png) |
| 4 | Ingresar | `/login` | [04-login.png](04-login.png) |
| 10 | Necesidades de la entidad | `/entidad/necesidades` | Pendiente |
| 11 | Donaciones asignadas | `/entidad/donaciones` | Pendiente |
| 12 | Confirmar recepción con fotos | `/entidad/donaciones/{id}/recepcion` | Pendiente |

Convención de nombres: `<req>[letra]-<pantalla>.png`. B y C agregan sus tablas (pantallas 5–9, 13–14 y 15–19) en este mismo archivo.
