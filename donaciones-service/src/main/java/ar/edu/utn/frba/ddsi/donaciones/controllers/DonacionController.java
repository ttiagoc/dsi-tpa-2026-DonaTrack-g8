package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import io.javalin.Javalin;

@Component
public class DonacionController implements JavalinController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/donaciones", ctx -> {
            ctx.json(donacionService.obtenerTodas());
        });

        app.get("/api/donaciones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(donacionService.obtenerPorId(id));
        });

        app.post("/api/donaciones", ctx -> {
            DonacionRequest request = ctx.bodyAsClass(DonacionRequest.class);
            ctx.status(201).json(donacionService.crear(request));
        });

        app.delete("/api/donaciones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            donacionService.eliminar(id);
            ctx.status(204);
        });

        app.put("/api/donaciones/{id}/estado", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            EstadoDonacionRequest request = ctx.bodyAsClass(EstadoDonacionRequest.class);
            ctx.json(donacionService.cambiarEstado(id, request));
        });

        app.get("/api/donaciones/estado/{estado}", ctx -> {
            String estado = ctx.pathParam("estado");
            int limit = Integer.parseInt(ctx.queryParam("limit"));
            String offsetStr = ctx.queryParam("offset");
            int offset = (offsetStr != null && !offsetStr.isBlank()) ? Integer.parseInt(offsetStr) : 0;
            ctx.json(donacionService.obtenerDonacionesSegunEstado(estado, limit, offset));
        });

        app.post("/api/donaciones/{id}/fotos", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            SubirFotosRecepcionRequest request = ctx.bodyAsClass(SubirFotosRecepcionRequest.class);
            donacionService.subirFotosRecepcion(id, request);
            ctx.status(204);
        });

        app.post("/api/donaciones/recepciones", ctx -> {
            ConfirmacionEntregaExitosaRequest request = ctx.bodyAsClass(ConfirmacionEntregaExitosaRequest.class);
            donacionService.confirmarEntregaExitosa(request);
            ctx.status(204);
        });
    }
}
