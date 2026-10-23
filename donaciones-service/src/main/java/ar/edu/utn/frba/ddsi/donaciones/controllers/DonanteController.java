package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.DONANTE;
import static ar.edu.utn.frba.ddsi.common.security.Rol.SISTEMA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import io.javalin.Javalin;

@Component
public class DonanteController implements JavalinController {

    private final DonanteService donanteService;
    private final GestorDeEventos gestorDeEventos;

    public DonanteController(DonanteService donanteService, GestorDeEventos gestorDeEventos) {
        this.donanteService = donanteService;
        this.gestorDeEventos = gestorDeEventos;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/donantes", ctx -> {
            ctx.json(donanteService.obtenerTodos());
        }, ADMINISTRADOR);

        app.get("/api/donantes/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, DONANTE, id);
            ctx.json(donanteService.obtenerPorId(id));
        }, DONANTE, ADMINISTRADOR);

        // el alta con usuario la hace /api/auth/registro/*; esta queda para el administrador (req. 15)
        app.post("/api/donantes/persona-humana", ctx -> {
            PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);
            ctx.status(201).json(donanteService.crearPersonaHumana(request));
        }, ADMINISTRADOR);

        app.post("/api/donantes/persona-juridica", ctx -> {
            PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);
            ctx.status(201).json(donanteService.crearPersonaJuridica(request));
        }, ADMINISTRADOR);

        app.put("/api/donantes/persona-humana/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, DONANTE, id);
            PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);
            ctx.json(donanteService.actualizarPersonaHumana(id, request));
        }, DONANTE, ADMINISTRADOR);

        app.put("/api/donantes/persona-juridica/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, DONANTE, id);
            PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);
            ctx.json(donanteService.actualizarPersonaJuridica(id, request));
        }, DONANTE, ADMINISTRADOR);

        app.delete("/api/donantes/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            if (!donanteService.eliminar(id)) {
                throw new ResourceNotFoundException("No se encontro un donante con el id: " + id);
            }
            ctx.status(204);
        }, ADMINISTRADOR);

        app.get("/api/donantes/{id}/contacto", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, DONANTE, id);
            ctx.json(donanteService.obtenerContactoPredeterminado(id));
        }, DONANTE, ADMINISTRADOR, SISTEMA);

        app.post("/api/donantes/inactividad/verificaciones", ctx -> {
            gestorDeEventos.verificarInactividadDonantes();
            ctx.status(204);
        }, ADMINISTRADOR);
    }
}
