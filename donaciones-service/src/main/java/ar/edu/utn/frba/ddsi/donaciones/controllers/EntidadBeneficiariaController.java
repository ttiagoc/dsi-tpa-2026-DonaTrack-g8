package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.ENTIDAD_BENEFICIARIA;
import static ar.edu.utn.frba.ddsi.common.security.Rol.SISTEMA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
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
        }, ADMINISTRADOR);

        app.get("/api/entidad-beneficiaria/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, id);
            ctx.json(entidadBeneficiariaService.obtenerPorId(id));
        }, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);

        // el alta con usuario la hace /api/auth/registro/entidad-beneficiaria
        app.post("/api/entidad-beneficiaria", ctx -> {
            EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);
            ctx.status(201).json(entidadBeneficiariaService.crear(request));
        }, ADMINISTRADOR);

        app.delete("/api/entidad-beneficiaria/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            if (!entidadBeneficiariaService.eliminar(id)) {
                throw new ResourceNotFoundException("No se encontro una entidad beneficiaria con el id: " + id);
            }
            ctx.status(204);
        }, ADMINISTRADOR);

        app.put("/api/entidad-beneficiaria/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, id);
            EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);
            ctx.json(entidadBeneficiariaService.actualizar(id, request));
        }, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);

        app.get("/api/entidad-beneficiaria/{entidadId}/necesidades", ctx -> {
            Long entidadId = Long.parseLong(ctx.pathParam("entidadId"));
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, entidadId);
            ctx.json(entidadBeneficiariaService.obtenerNecesidades(entidadId));
        }, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);

        app.post("/api/entidad-beneficiaria/{entidadId}/necesidades", ctx -> {
            Long entidadId = Long.parseLong(ctx.pathParam("entidadId"));
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, entidadId);
            NecesidadRequest request = ctx.bodyAsClass(NecesidadRequest.class);
            ctx.status(201).json(entidadBeneficiariaService.registrarNecesidad(entidadId, request));
        }, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);

        app.delete("/api/entidad-beneficiaria/{entidadId}/necesidades/{necesidadId}", ctx -> {
            Long entidadId = Long.parseLong(ctx.pathParam("entidadId"));
            Autorizacion.exigirPropietario(ctx, ENTIDAD_BENEFICIARIA, entidadId);
            Long necesidadId = Long.parseLong(ctx.pathParam("necesidadId"));
            entidadBeneficiariaService.eliminarNecesidad(entidadId, necesidadId);
            ctx.status(204);
        }, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);

        app.get("/api/entidad-beneficiaria/{id}/contactos", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(entidadBeneficiariaService.obtenerContactos(id));
        }, ADMINISTRADOR, SISTEMA);
    }
}
