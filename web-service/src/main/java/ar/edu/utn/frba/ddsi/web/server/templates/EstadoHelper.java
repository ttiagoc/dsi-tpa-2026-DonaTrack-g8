package ar.edu.utn.frba.ddsi.web.server.templates;

import java.io.IOException;
import java.util.Map;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Helper {{estado valor}} para Handlebars.
 * Formatea cualquier estado de donación en texto legible y con la clase de etiqueta correspondiente.
 */
public class EstadoHelper implements Helper<Object> {

    @Override
    public Object apply(Object context, Options options) throws IOException {
        if (context == null) {
            return new Handlebars.SafeString("<span class=\"etiqueta etiqueta--neutra\">-</span>");
        }

        if (context instanceof Map<?, ?> map && map.containsKey("estado")) {
            context = map.get("estado");
        } else if (!(context instanceof CharSequence || context instanceof Enum)) {
            try {
                var method = context.getClass().getMethod("estado");
                context = method.invoke(context);
            } catch (Exception e1) {
                try {
                    var method2 = context.getClass().getMethod("getEstado");
                    context = method2.invoke(context);
                } catch (Exception e2) {
                    // ignore
                }
            }
        }

        if (context == null) {
            return new Handlebars.SafeString("<span class=\"etiqueta etiqueta--neutra\">-</span>");
        }

        String raw = context.toString().trim();
        String estado = raw.toUpperCase().replace('-', '_');

        String texto;
        String clase;

        switch (estado) {
            case "EN_DEPOSITO" -> {
                texto = "En depósito";
                clase = "etiqueta";
            }
            case "ASIGNACION_REALIZADA", "ASIGNADA" -> {
                texto = "Asignación realizada";
                clase = "etiqueta etiqueta--info";
            }
            case "LISTA_PARA_ENTREGAR" -> {
                texto = "Lista para entregar";
                clase = "etiqueta etiqueta--info";
            }
            case "EN_TRASLADO", "EN_CAMINO" -> {
                texto = "En traslado";
                clase = "etiqueta etiqueta--pendiente";
            }
            case "ENTREGADA" -> {
                texto = "Entregada";
                clase = "etiqueta etiqueta--ok";
            }
            case "RECIBIDA" -> {
                texto = "Recibida";
                clase = "etiqueta etiqueta--ok";
            }
            case "ENTREGA_FALLIDA" -> {
                texto = "Entrega fallida";
                clase = "etiqueta etiqueta--error";
            }
            case "VENCIDA" -> {
                texto = "Vencida";
                clase = "etiqueta etiqueta--error";
            }
            case "PLANIFICADA" -> {
                texto = "Planificada";
                clase = "etiqueta etiqueta--pendiente";
            }
            case "FINALIZADA" -> {
                texto = "Finalizada";
                clase = "etiqueta etiqueta--ok";
            }
            case "PENDIENTE" -> {
                texto = "Pendiente";
                clase = "etiqueta etiqueta--neutra";
            }
            case "NO_RECIBIDA" -> {
                texto = "No recibida";
                clase = "etiqueta etiqueta--error";
            }
            default -> {
                texto = raw;
                clase = "etiqueta etiqueta--neutra";
            }
        }

        return new Handlebars.SafeString("<span class=\"" + clase + "\">" + escapeHtml(texto) + "</span>");
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
