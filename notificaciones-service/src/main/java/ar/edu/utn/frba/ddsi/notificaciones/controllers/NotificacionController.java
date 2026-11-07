package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
import ar.edu.utn.frba.ddsi.common.security.Rol;
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
            String pageStr = ctx.queryParam("page");
            String sizeStr = ctx.queryParam("size");

            if (destinatarioIdStr != null && tipoDestinatario != null) {
                Long destinatarioId = Long.parseLong(destinatarioIdStr);
                Rol rolRequerido = "ENTIDAD".equalsIgnoreCase(tipoDestinatario)
                        ? Rol.ENTIDAD_BENEFICIARIA
                        : Rol.DONANTE;
                Autorizacion.exigirPropietario(ctx, rolRequerido, destinatarioId);
                if (pageStr != null || sizeStr != null) {
                    int page = (pageStr != null && !pageStr.isBlank()) ? Integer.parseInt(pageStr) : 1;
                    int size = (sizeStr != null && !sizeStr.isBlank()) ? Integer.parseInt(sizeStr) : 10;
                    ctx.json(notificacionRepository.findByDestinatarioPaginado(destinatarioId, tipoDestinatario, page, size));
                } else {
                    ctx.json(notificacionRepository.findByDestinatario(destinatarioId, tipoDestinatario));
                }
            } else {
                Autorizacion.exigir(Autorizacion.esPrivilegiado(ctx));
                if (pageStr != null || sizeStr != null) {
                    int page = (pageStr != null && !pageStr.isBlank()) ? Integer.parseInt(pageStr) : 1;
                    int size = (sizeStr != null && !sizeStr.isBlank()) ? Integer.parseInt(sizeStr) : 10;
                    ctx.json(notificacionRepository.findAllPaginado(page, size));
                } else {
                    ctx.json(notificacionRepository.findAll());
                }
            }
        }, Rol.DONANTE, Rol.ENTIDAD_BENEFICIARIA, Rol.ADMINISTRADOR, Rol.SISTEMA);
    }
}
