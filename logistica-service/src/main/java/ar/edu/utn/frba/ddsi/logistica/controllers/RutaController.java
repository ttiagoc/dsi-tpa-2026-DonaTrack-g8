package ar.edu.utn.frba.ddsi.logistica.controllers;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logistica.services.RutaService;
import io.javalin.Javalin;

@Component
public class RutaController implements JavalinController {

    private final RutaService rutaService;
    private final PlanificadorDeRutas planificadorDeRutas;

    public RutaController(RutaService rutaService, PlanificadorDeRutas planificadorDeRutas) {
        this.rutaService = rutaService;
        this.planificadorDeRutas = planificadorDeRutas;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/rutas", ctx -> {
            String fechaStr = ctx.queryParam("fecha");
            LocalDate fecha = (fechaStr != null && !fechaStr.isBlank()) ? LocalDate.parse(fechaStr) : null;
            ctx.json(rutaService.obtenerTodas(fecha));
        });

        app.get("/api/rutas/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(rutaService.obtenerPorId(id));
        });

        app.post("/api/rutas", ctx -> {
            RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
            ctx.status(201).json(rutaService.crear(request));
        });

        app.put("/api/rutas/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
            ctx.json(rutaService.actualizar(id, request));
        });

        app.delete("/api/rutas/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            rutaService.eliminar(id);
            ctx.status(204);
        });

        app.put("/api/rutas/{id}/estado", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            String estado = ctx.body();
            if (estado.startsWith("\"") && estado.endsWith("\"")) {
                estado = estado.substring(1, estado.length() - 1);
            }
            rutaService.actualizarEstado(id, estado);
            ctx.status(204);
        });

        app.post("/api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones", ctx -> {
            Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
            Long paradaId = Long.parseLong(ctx.pathParam("paradaId"));
            rutaService.confirmarEntregaExitosa(rutaId, paradaId);
            ctx.status(204);
        });

        app.get("/api/rutas/{id}/ubicacion", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(rutaService.obtenerUbicacionActual(id));
        });

        app.post("/api/rutas/planificaciones", ctx -> {
            planificadorDeRutas.planificarRutas();
            ctx.status(204);
        });

        app.post("/api/rutas/planificaciones/resultados", ctx -> {
            EjecutarPlanificacionRequest request = ctx.bodyAsClass(EjecutarPlanificacionRequest.class);
            planificadorDeRutas.ejecutarPlanificacion(request);
            ctx.status(204);
        });
    }
}
