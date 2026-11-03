package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificador;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;
import io.javalin.Javalin;

@Component
public class NotificacionController implements JavalinController {

    private final Notificador notificador;
    private final NotificacionRepository notificacionRepository;

    public NotificacionController(Notificador notificador, NotificacionRepository notificacionRepository) {
        this.notificador = notificador;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.post("/api/notificaciones", ctx -> {
            NotificacionRequest request = ctx.bodyAsClass(NotificacionRequest.class);
            notificador.enviarNotificacion(request);
            ctx.status(200);
        });

        app.get("/api/notificaciones", ctx -> {
          String destinatarioIdStr = ctx.queryParam("destinatarioId");
          String tipoDestinatario = ctx.queryParam("tipoDestinatario");

          List<Notificacion> resultado;
          if(destinatarioIdStr != null && tipoDestinatario != null){
            Long destinatarioId = Long.parseLong(destinatarioIdStr);
            resultado = notificacionRepository.findByDestinatario(destinatarioId, tipoDestinatario);
          } else {
            resultado = notificacionRepository.findAll();
          }

          ctx.json(resultado);
        });
    }
}
