package ar.edu.utn.frba.ddsi.web.controllers;

import org.springframework.stereotype.Component;

import io.javalin.http.Context;

/**
 * Pagina de entrada de cada rol despues del login: un saludo y una tarjeta por cada pantalla del rol
 * (las mismas que el menu lateral). B: /donante, C: /admin, A: /entidad.
 */
@Component
public class PanelController {

    public void donante(Context ctx) {
        ctx.render("donante/panel.html.hbs", Modelo.base(ctx));
    }

    public void entidad(Context ctx) {
        ctx.render("entidad/panel.html.hbs", Modelo.base(ctx));
    }

    public void admin(Context ctx) {
        ctx.render("admin/panel.html.hbs", Modelo.base(ctx));
    }
}
