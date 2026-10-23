package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas.Alerta;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import ar.edu.utn.frba.ddsi.web.sesion.UsuarioVista;
import io.javalin.http.Context;

/**
 * Todo controller arranca el modelo de su vista desde aca, asi el layout siempre tiene:
 * - usuario: el usuario logueado (UsuarioVista) o nada si es un visitante;
 * - alertas: los mensajes pendientes (ver Alertas).
 */
public final class Modelo {

    private Modelo() {
    }

    public static Map<String, Object> base(Context ctx) {
        Map<String, Object> modelo = new HashMap<>();
        SesionUsuario.actual(ctx).ifPresent(sesion -> modelo.put("usuario", UsuarioVista.de(sesion.getUsuario())));
        modelo.put("alertas", new ArrayList<>(Alertas.consumir(ctx)));
        return modelo;
    }

    /** Para mostrar una alerta en la misma respuesta, sin pasar por un redirect. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> conAlerta(Map<String, Object> modelo, String tipo, String mensaje) {
        ((List<Alerta>) modelo.get("alertas")).add(new Alerta(tipo, mensaje));
        return modelo;
    }
}
