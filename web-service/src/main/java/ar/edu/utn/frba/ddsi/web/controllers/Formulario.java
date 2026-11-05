package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.javalin.http.Context;

/**
 * Lee un formulario HTML y junta los errores por campo para volver a mostrarlo.
 * En la plantilla: value="{{form.campo}}" y {{errores.campo}}. Las contrasenias nunca vuelven a la vista.
 * Las reglas de email y telefono son las mismas que valida el navegador (public/js/formularios.js).
 */
public class Formulario {

    private static final Set<String> SECRETOS = Set.of("password", "password2", "choferPassword");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9\\s()\\-]+$");
    public static final String ERROR_EMAIL = "Ingresá un email válido, por ejemplo nombre@dominio.com";
    public static final String ERROR_TELEFONO = "Ingresá un teléfono válido: de 8 a 15 números (podés usar +, espacios y guiones)";

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

    public String email(String campo, String etiqueta, boolean obligatorio) {
        String valor = obligatorio ? requerido(campo, etiqueta) : valor(campo);
        if (valor != null && !EMAIL.matcher(valor).matches()) {
            error(campo, ERROR_EMAIL);
        }
        return valor;
    }

    public String telefono(String campo, String etiqueta, boolean obligatorio) {
        String valor = obligatorio ? requerido(campo, etiqueta) : valor(campo);
        if (valor != null) {
            int digitos = valor.replaceAll("\\D", "").length();
            if (!TELEFONO.matcher(valor).matches() || digitos < 8 || digitos > 15) {
                error(campo, ERROR_TELEFONO);
            }
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

    public Map<String, String> errores() {
        return errores;
    }

    public Map<String, String> valores() {
        return valores;
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
