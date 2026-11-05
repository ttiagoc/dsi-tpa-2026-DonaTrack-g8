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
import java.io.InputStream;
import io.javalin.http.UploadedFile;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.ImportacionTarea;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.ImportadorDeDonantes;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import io.javalin.Javalin;

@Component
public class DonanteController implements JavalinController {

    private final DonanteService donanteService;
    private final GestorDeEventos gestorDeEventos;
    private final ImportadorDeDonantes importadorDeDonantes;

    public DonanteController(DonanteService donanteService, GestorDeEventos gestorDeEventos,
                ImportadorDeDonantes importadorDeDonantes) {
        this.donanteService = donanteService;
        this.gestorDeEventos = gestorDeEventos;
        this.importadorDeDonantes = importadorDeDonantes;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/donantes/importaciones", ctx -> {
            ctx.json(importadorDeDonantes.obtenerImportaciones().stream().map(ImportacionTarea::toResponse).toList());
        }, ADMINISTRADOR);

        app.get("/api/donantes/importaciones/{id}", ctx -> {
            String id = ctx.pathParam("id");
            ImportacionTarea tarea = importadorDeDonantes.obtenerImportacion(id)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontro la importacion: " + id));
            ctx.json(tarea.toResponse());
        }, ADMINISTRADOR);

        app.post("/api/donantes/importaciones", ctx -> {
            String filename = ctx.header("X-Filename");
            InputStream is;
            UploadedFile uploadedFile = ctx.uploadedFile("archivo");
            if (uploadedFile != null) {
                filename = uploadedFile.filename();
                is = uploadedFile.content();
            } else {
                if (filename == null || filename.isBlank()) {
                    filename = "donantes.csv";
                }
                is = ctx.bodyInputStream();
            }
            ImportacionTarea tarea = importadorDeDonantes.iniciarImportacion(is, filename);
            ctx.status(202).json(tarea.toResponse());
        }, ADMINISTRADOR);

        app.get("/api/donantes", ctx -> {
            ctx.json(donanteService.obtenerTodos());
        }, ADMINISTRADOR);

        app.get("/api/donantes/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Autorizacion.exigirPropietario(ctx, DONANTE, id);
            ctx.json(donanteService.obtenerPorId(id));
        }, DONANTE, ADMINISTRADOR);

        // datos completos para la pantalla de donantes del administrador (ver y editar)
        app.get("/api/donantes/{id}/detalle", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(donanteService.obtenerDetalle(id));
        }, ADMINISTRADOR);

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
