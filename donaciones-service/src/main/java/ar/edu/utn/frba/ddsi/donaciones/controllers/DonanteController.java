package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.DONANTE;
import static ar.edu.utn.frba.ddsi.common.security.Rol.SISTEMA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.AccesoInicialDonante;
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
    private final AccesoInicialDonante accesoInicial;

    public DonanteController(DonanteService donanteService, GestorDeEventos gestorDeEventos,
                ImportadorDeDonantes importadorDeDonantes, AccesoInicialDonante accesoInicial) {
        this.donanteService = donanteService;
        this.gestorDeEventos = gestorDeEventos;
        this.importadorDeDonantes = importadorDeDonantes;
        this.accesoInicial = accesoInicial;
    }

    // el correo obligatorio del donante es su nombre de usuario y adonde van las credenciales
    private static String emailDe(List<MedioContactoRequest> contactos) {
        if (contactos == null) {
            return null;
        }
        return contactos.stream()
                .filter(c -> c != null && "EMAIL".equalsIgnoreCase(c.tipo()))
                .map(MedioContactoRequest::valor)
                .findFirst()
                .orElse(null);
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
            String pageStr = ctx.queryParam("page");
            String sizeStr = ctx.queryParam("size");
            if (pageStr != null || sizeStr != null) {
                int page = (pageStr != null && !pageStr.isBlank()) ? Integer.parseInt(pageStr) : 1;
                int size = (sizeStr != null && !sizeStr.isBlank()) ? Integer.parseInt(sizeStr) : 10;
                ctx.json(donanteService.obtenerTodosPaginado(page, size));
            } else {
                ctx.json(donanteService.obtenerTodos());
            }
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

        // el alta con usuario la hace /api/auth/registro/*; esta queda para el administrador (req. 15):
        // el donante que se carga en el deposito recibe su usuario y la bienvenida con las credenciales
        app.post("/api/donantes/persona-humana", ctx -> {
            PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);
            DonanteResponse creado = donanteService.crearPersonaHumana(request);
            String nombre = (request.nombre() != null ? request.nombre() : "")
                    + (request.apellido() != null ? " " + request.apellido() : "");
            accesoInicial.otorgar(creado.id(), nombre, emailDe(request.contactos()), true);
            ctx.status(201).json(creado);
        }, ADMINISTRADOR);

        app.post("/api/donantes/persona-juridica", ctx -> {
            PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);
            DonanteResponse creado = donanteService.crearPersonaJuridica(request);
            accesoInicial.otorgar(creado.id(), request.razonSocial(), emailDe(request.contactos()), true);
            ctx.status(201).json(creado);
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

        // a demanda: avisa a todos los donantes que llevan mas de 20 dias sin donar
        app.post("/api/donantes/inactividad/verificaciones", ctx -> {
            gestorDeEventos.verificarInactividadDonantes();
            ctx.status(204);
        }, ADMINISTRADOR);

        // escaneo diario que dispara el cron de la VM: avisa solo a quienes hoy cumplen 20, 40, ... dias sin donar
        app.post("/api/donantes/inactividad/verificaciones-diarias", ctx -> {
            gestorDeEventos.verificarInactividadDiaria();
            ctx.status(204);
        }, SISTEMA, ADMINISTRADOR);
    }
}
