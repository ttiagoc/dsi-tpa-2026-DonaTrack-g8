package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
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
        });

        app.get("/api/donantes/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(donanteService.obtenerPorId(id));
        });

        app.post("/api/donantes/persona-humana", ctx -> {
            PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);
            ctx.status(201).json(donanteService.crearPersonaHumana(request));
        });

        app.post("/api/donantes/persona-juridica", ctx -> {
            PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);
            ctx.status(201).json(donanteService.crearPersonaJuridica(request));
        });

        app.put("/api/donantes/persona-humana/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);
            ctx.json(donanteService.actualizarPersonaHumana(id, request));
        });

        app.put("/api/donantes/persona-juridica/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);
            ctx.json(donanteService.actualizarPersonaJuridica(id, request));
        });

        app.delete("/api/donantes/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            donanteService.eliminar(id);
            ctx.status(204);
        });

        app.get("/api/donantes/{id}/contacto", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(donanteService.obtenerContactoPredeterminado(id));
        });

        app.post("/api/donantes/inactividad/verificaciones", ctx -> {
            gestorDeEventos.verificarInactividadDonantes();
            ctx.status(204);
        });
    }
}
