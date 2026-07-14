package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import io.javalin.Javalin;

@Component
public class EntidadBeneficiariaController implements JavalinController {

    private final EntidadBeneficiariaService entidadBeneficiariaService;

    public EntidadBeneficiariaController(EntidadBeneficiariaService entidadBeneficiariaService) {
        this.entidadBeneficiariaService = entidadBeneficiariaService;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/entidad-beneficiaria", ctx -> {
            ctx.json(entidadBeneficiariaService.obtenerTodas());
        });

        app.get("/api/entidad-beneficiaria/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(entidadBeneficiariaService.obtenerPorId(id));
        });

        app.post("/api/entidad-beneficiaria", ctx -> {
            EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);
            ctx.status(201).json(entidadBeneficiariaService.crear(request));
        });

        app.delete("/api/entidad-beneficiaria/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            entidadBeneficiariaService.eliminar(id);
            ctx.status(204);
        });

        app.put("/api/entidad-beneficiaria/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);
            ctx.json(entidadBeneficiariaService.actualizar(id, request));
        });

        app.get("/api/entidad-beneficiaria/{entidadId}/necesidades", ctx -> {
            Long entidadId = Long.parseLong(ctx.pathParam("entidadId"));
            ctx.json(entidadBeneficiariaService.obtenerNecesidades(entidadId));
        });

        app.post("/api/entidad-beneficiaria/{entidadId}/necesidades", ctx -> {
            Long entidadId = Long.parseLong(ctx.pathParam("entidadId"));
            NecesidadRequest request = ctx.bodyAsClass(NecesidadRequest.class);
            ctx.status(201).json(entidadBeneficiariaService.registrarNecesidad(entidadId, request));
        });

        app.delete("/api/entidad-beneficiaria/{entidadId}/necesidades/{necesidadId}", ctx -> {
            Long entidadId = Long.parseLong(ctx.pathParam("entidadId"));
            Long necesidadId = Long.parseLong(ctx.pathParam("necesidadId"));
            entidadBeneficiariaService.eliminarNecesidad(entidadId, necesidadId);
            ctx.status(204);
        });

        app.get("/api/entidad-beneficiaria/{id}/contactos", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(entidadBeneficiariaService.obtenerContactos(id));
        });
    }
}
