package ar.edu.utn.frba.ddsi.web.server.templates;

import java.io.IOException;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Helper {{fecha valor [hora=true]}} para Handlebars.
 * Formatea fechas a dd/MM/yyyy (y opcionalmente HH:mm).
 */
public class FechaHelper implements Helper<Object> {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public Object apply(Object context, Options options) throws IOException {
        if (context == null) {
            return "";
        }

        if (context instanceof Map<?, ?> map && map.containsKey("fecha")) {
            context = map.get("fecha");
        } else if (!(context instanceof TemporalAccessor || context instanceof Instant || context instanceof CharSequence || context instanceof Number)) {
            try {
                var method = context.getClass().getMethod("fecha");
                context = method.invoke(context);
            } catch (Exception e1) {
                try {
                    var method2 = context.getClass().getMethod("getFecha");
                    context = method2.invoke(context);
                } catch (Exception e2) {
                    // ignore
                }
            }
        }

        if (context == null) {
            return "";
        }

        Object conHoraVal = options.hash("hora", null);
        boolean conHora = conHoraVal != null && (Boolean.TRUE.equals(conHoraVal) || "true".equalsIgnoreCase(conHoraVal.toString()));

        if (context instanceof LocalDate ld) {
            return ld.format(FECHA_FMT);
        }
        if (context instanceof LocalDateTime ldt) {
            return conHora ? ldt.format(FECHA_HORA_FMT) : ldt.format(FECHA_FMT);
        }
        if (context instanceof ZonedDateTime zdt) {
            return conHora ? zdt.format(FECHA_HORA_FMT) : zdt.format(FECHA_FMT);
        }
        if (context instanceof OffsetDateTime odt) {
            return conHora ? odt.format(FECHA_HORA_FMT) : odt.format(FECHA_FMT);
        }
        if (context instanceof Instant inst) {
            LocalDateTime ldt = LocalDateTime.ofInstant(inst, ZoneId.systemDefault());
            return conHora ? ldt.format(FECHA_HORA_FMT) : ldt.format(FECHA_FMT);
        }

        String str = context.toString().trim();
        if (str.isEmpty()) {
            return "";
        }

        // Si ya viene formateado tipo dd/MM/yyyy
        if (str.matches("^\\d{2}/\\d{2}/\\d{4}( \\d{2}:\\d{2})?$")) {
            return str;
        }

        // Intenta parsear ISO
        try {
            if (str.contains("T")) {
                LocalDateTime ldt = LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
                return conHora ? ldt.format(FECHA_HORA_FMT) : ldt.format(FECHA_FMT);
            } else if (str.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                LocalDate ld = LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
                return ld.format(FECHA_FMT);
            }
        } catch (DateTimeParseException ignored) {
        }

        return str;
    }
}
