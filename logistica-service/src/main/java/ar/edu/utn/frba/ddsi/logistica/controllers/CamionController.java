package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;
import ar.edu.utn.frba.ddsi.logistica.services.CamionService;
import io.javalin.Javalin;

@Component
public class CamionController implements JavalinController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/camiones", ctx -> {
            ctx.json(camionService.obtenerTodos());
        });

        app.get("/api/camiones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(camionService.obtenerPorId(id));
        });

        app.post("/api/camiones", ctx -> {
            CamionRequest request = ctx.bodyAsClass(CamionRequest.class);
            ctx.status(201).json(camionService.crear(request));
        });

        app.put("/api/camiones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            CamionRequest request = ctx.bodyAsClass(CamionRequest.class);
            ctx.json(camionService.actualizar(id, request));
        });

        app.delete("/api/camiones/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            camionService.eliminar(id);
            ctx.status(204);
        });

        app.get("/api/camiones/activos", ctx -> {
            ctx.json(camionService.obtenerCamionesActivos());
        });

        app.post("/api/camiones/ubicacion/{patente}", ctx -> {
            String patente = ctx.pathParam("patente");
            UbicacionRequest request = ctx.bodyAsClass(UbicacionRequest.class);
            camionService.recibirTelemetria(patente, request);
            ctx.status(204);
        });
    }
}
