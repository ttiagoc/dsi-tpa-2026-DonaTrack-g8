package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.javalin.http.Context;

@Component
public class HomeController {

    public void index(Context ctx) {
        Map<String, Object> modelo = Modelo.base(ctx);
        if (ctx.queryParam("logout") != null) {
            Modelo.conAlerta(modelo, "info", "Cerraste sesión. ¡Hasta pronto!");
        }
        ctx.render("home/index.html.hbs", modelo);
    }

    public void legal(Context ctx) {
        ctx.render("home/legal.html.hbs", Modelo.base(ctx));
    }
}
