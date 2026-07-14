package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificador;
import io.javalin.Javalin;

@Component
public class NotificacionController implements JavalinController {

    private final Notificador notificador;

    public NotificacionController(Notificador notificador) {
        this.notificador = notificador;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.post("/api/notificaciones", ctx -> {
            NotificacionRequest request = ctx.bodyAsClass(NotificacionRequest.class);
            notificador.enviarNotificacion(request);
            ctx.status(200);
        });
    }
}
