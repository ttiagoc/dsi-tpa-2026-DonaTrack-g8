package ar.edu.utn.frba.ddsi.web.server.templates;

import java.io.IOException;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Helper {{boton "<tipo>" ...}} para Handlebars.
 * Genera enlaces o botones con clases CSS, variantes, íconos y accesibilidad estandarizados.
 */
public class BotonHelper implements Helper<Object> {

    @Override
    public Object apply(Object context, Options options) throws IOException {
        TipoBoton tipo = TipoBoton.desde(context);

        Object textoVal = options.hash("texto");
        String texto = textoVal != null ? textoVal.toString() : defaultTexto(tipo);
        Object hrefVal = options.hash("href");
        String href = hrefVal != null ? hrefVal.toString() : null;
        Object modalVal = options.hash("modal");
        String modal = modalVal != null ? modalVal.toString() : null;
        Object cierraVal = options.hash("cierra");
        boolean cierra = cierraVal != null && (Boolean.TRUE.equals(cierraVal) || "true".equalsIgnoreCase(cierraVal.toString()));
        Object srVal = options.hash("sr");
        String sr = srVal != null ? srVal.toString() : null;
        Object iconoVal = options.hash("icono");
        String icono = iconoVal != null ? iconoVal.toString() : tipo.getIcono();
        Object cargandoVal = options.hash("cargando");
        String cargando = cargandoVal != null ? cargandoVal.toString() : null;
        Object extraClaseVal = options.hash("clase");
        String extraClase = extraClaseVal != null ? extraClaseVal.toString() : null;
        Object formactionVal = options.hash("formaction");
        String formaction = formactionVal != null ? formactionVal.toString() : null;
        Object autofocusVal = options.hash("autofocus");
        boolean autofocus = autofocusVal != null && (Boolean.TRUE.equals(autofocusVal) || "true".equalsIgnoreCase(autofocusVal.toString()));

        String clases = extraClase != null && !extraClase.isBlank()
                ? tipo.getClase() + " " + extraClase.trim()
                : tipo.getClase();

        StringBuilder sb = new StringBuilder();

        if (href != null && !href.isBlank()) {
            sb.append("<a class=\"").append(escapeHtml(clases)).append("\" href=\"").append(escapeHtml(href)).append("\"");
            if (modal != null && !modal.isBlank()) {
                sb.append(" data-abre=\"").append(escapeHtml(modal)).append("\"");
            }
            if (cierra) {
                sb.append(" data-cierra");
            }
            if (cargando != null && !cargando.isBlank()) {
                sb.append(" data-cargando=\"").append(escapeHtml(cargando)).append("\"");
            }
            if (autofocus) {
                sb.append(" autofocus");
            }
            sb.append(">");
            appendContenido(sb, icono, texto, sr);
            sb.append("</a>");
        } else {
            String defaultType = (tipo == TipoBoton.CONFIRMAR || tipo == TipoBoton.CONFIRMAR_PELIGRO) ? "submit" : "button";
            String type = options.hash("type", defaultType);

            sb.append("<button type=\"").append(escapeHtml(type)).append("\" class=\"").append(escapeHtml(clases)).append("\"");
            if (modal != null && !modal.isBlank()) {
                sb.append(" data-abre=\"").append(escapeHtml(modal)).append("\"");
            }
            if (cierra) {
                sb.append(" data-cierra");
            }
            if (formaction != null && !formaction.isBlank()) {
                sb.append(" formaction=\"").append(escapeHtml(formaction)).append("\"");
            }
            if (cargando != null && !cargando.isBlank()) {
                sb.append(" data-cargando=\"").append(escapeHtml(cargando)).append("\"");
            }
            if (autofocus) {
                sb.append(" autofocus");
            }
            sb.append(">");
            appendContenido(sb, icono, texto, sr);
            sb.append("</button>");
        }

        return new Handlebars.SafeString(sb.toString());
    }

    private void appendContenido(StringBuilder sb, String icono, String texto, String sr) {
        if (icono != null && !icono.isBlank()) {
            sb.append("<svg class=\"icono\" aria-hidden=\"true\"><use href=\"#")
              .append(escapeHtml(icono))
              .append("\"/></svg>");
            if (texto != null && !texto.isBlank()) {
                sb.append(" ");
            }
        }
        if (texto != null && !texto.isBlank()) {
            sb.append(escapeHtml(texto));
        }
        if (sr != null && !sr.isBlank()) {
            sb.append(" <span class=\"sr-only\">").append(escapeHtml(sr)).append("</span>");
        }
    }

    private String defaultTexto(TipoBoton tipo) {
        return switch (tipo) {
            case ALTA -> "+ Nuevo";
            case VER -> "Ver";
            case EDITAR -> "Editar";
            case ELIMINAR -> "Eliminar";
            case CONFIRMAR, CONFIRMAR_PELIGRO -> "Confirmar";
            case CANCELAR -> "Cancelar";
            case PAGINA -> "";
        };
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
