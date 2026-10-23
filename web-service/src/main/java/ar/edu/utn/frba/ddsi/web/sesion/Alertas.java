package ar.edu.utn.frba.ddsi.web.sesion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import io.javalin.http.Context;

/**
 * Mensajes "flash": se guardan antes de un redirect y se muestran (una sola vez) en la pagina siguiente.
 * Es lo que permite el patron POST -> redirect -> GET sin perder el "se guardo con exito".
 */
public final class Alertas {

    private static final String ATRIBUTO = "donatrack.alertas";

    public record Alerta(String tipo, String mensaje) implements Serializable {
    }

    private Alertas() {
    }

    public static void exito(Context ctx, String mensaje) {
        agregar(ctx, new Alerta("exito", mensaje));
    }

    public static void error(Context ctx, String mensaje) {
        agregar(ctx, new Alerta("error", mensaje));
    }

    public static void info(Context ctx, String mensaje) {
        agregar(ctx, new Alerta("info", mensaje));
    }

    @SuppressWarnings("unchecked")
    private static void agregar(Context ctx, Alerta alerta) {
        HttpSession sesion = ctx.req().getSession(true);
        List<Alerta> alertas = (List<Alerta>) sesion.getAttribute(ATRIBUTO);
        if (alertas == null) {
            alertas = new ArrayList<>();
        }
        alertas.add(alerta);
        sesion.setAttribute(ATRIBUTO, alertas);
    }

    /** Devuelve las alertas pendientes y las borra. */
    @SuppressWarnings("unchecked")
    public static List<Alerta> consumir(Context ctx) {
        HttpSession sesion = ctx.req().getSession(false);
        if (sesion == null || sesion.getAttribute(ATRIBUTO) == null) {
            return List.of();
        }
        List<Alerta> alertas = (List<Alerta>) sesion.getAttribute(ATRIBUTO);
        sesion.removeAttribute(ATRIBUTO);
        return alertas;
    }
}
