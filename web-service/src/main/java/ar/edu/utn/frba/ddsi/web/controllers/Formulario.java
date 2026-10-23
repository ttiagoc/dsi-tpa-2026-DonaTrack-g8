package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.javalin.http.Context;

/**
 * Lee un formulario HTML y junta los errores por campo para volver a mostrarlo.
 * En la plantilla: value="{{form.campo}}" y {{errores.campo}}. Las contrasenias nunca vuelven a la vista.
 */
public class Formulario {

    private static final Set<String> SECRETOS = Set.of("password", "password2");

    private final Context ctx;
    private final Map<String, String> valores = new LinkedHashMap<>();
    private final Map<String, String> errores = new LinkedHashMap<>();

    public Formulario(Context ctx) {
        this.ctx = ctx;
        ctx.formParamMap().forEach((campo, lista) -> {
            if (!SECRETOS.contains(campo) && !lista.isEmpty()) {
                valores.put(campo, lista.get(0).trim());
            }
        });
    }

    /** El valor sin espacios alrededor, o null si vino vacio. */
    public String valor(String campo) {
        if (SECRETOS.contains(campo)) {
            String secreto = ctx.formParam(campo);
            return secreto == null || secreto.isEmpty() ? null : secreto;
        }
        String valor = valores.get(campo);
        return valor == null || valor.isEmpty() ? null : valor;
    }

    public String requerido(String campo, String etiqueta) {
        String valor = valor(campo);
        if (valor == null) {
            error(campo, etiqueta + " es obligatorio");
        }
        return valor;
    }

    public LocalDate fecha(String campo, String etiqueta) {
        String valor = requerido(campo, etiqueta);
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            error(campo, etiqueta + " no es una fecha valida");
            return null;
        }
    }

    public void error(String campo, String mensaje) {
        errores.putIfAbsent(campo, mensaje);
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    /** Agrega form y errores al modelo, mas un error general arriba del formulario si hay. */
    public Map<String, Object> enModelo(Map<String, Object> modelo, String errorGeneral) {
        modelo.put("form", valores);
        modelo.put("errores", errores);
        if (errorGeneral != null) {
            modelo.put("error", errorGeneral);
        }
        return modelo;
    }
}
