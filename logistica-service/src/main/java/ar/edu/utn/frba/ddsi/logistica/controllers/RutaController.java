package ar.edu.utn.frba.ddsi.logistica.controllers;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.CHOFER;
import static ar.edu.utn.frba.ddsi.common.security.Rol.SISTEMA;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logistica.services.RutaService;
import io.javalin.Javalin;
import io.javalin.http.Context;

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

        app.get("/api/rutas/activas", ctx -> {
            String entidadIdStr = ctx.queryParam("entidadId");
            Long entidadId = (entidadIdStr != null && !entidadIdStr.isBlank()) ? Long.parseLong(entidadIdStr) : null;
            java.util.List<Long> donacionIds = null;
            String donacionesStr = ctx.queryParam("donacionIds");
            if (donacionesStr != null && !donacionesStr.isBlank()) {
                donacionIds = java.util.Arrays.stream(donacionesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .toList();
            }
            ctx.json(rutaService.obtenerRutasActivas(entidadId, donacionIds));
        });

        app.get("/api/rutas/camion/{camionId}/activa", ctx -> {
            Long camionId = Long.parseLong(ctx.pathParam("camionId"));
            Autorizacion.exigirPropietario(ctx, CHOFER, camionId);
            var activa = rutaService.obtenerRutaActivaPorCamion(camionId);
            if (activa.isPresent()) {
                ctx.json(activa.get());
            } else {
                ctx.status(204);
            }
        }, CHOFER, ADMINISTRADOR, SISTEMA);

        app.get("/api/rutas/camion/{camionId}", ctx -> {
            Long camionId = Long.parseLong(ctx.pathParam("camionId"));
            Autorizacion.exigirPropietario(ctx, CHOFER, camionId);
            ctx.json(rutaService.obtenerPorCamion(camionId));
        }, CHOFER, ADMINISTRADOR, SISTEMA);

        app.get("/api/rutas/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(rutaService.obtenerPorId(id));
        });

        app.post("/api/rutas", ctx -> {
            RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
            ctx.status(201).json(rutaService.crear(request));
        }, ADMINISTRADOR, SISTEMA);

        app.put("/api/rutas/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
            ctx.json(rutaService.actualizar(id, request));
        }, ADMINISTRADOR, SISTEMA);

        app.delete("/api/rutas/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            if (!rutaService.eliminar(id)) {
                throw new ResourceNotFoundException("No se encontro una ruta con el id: " + id);
            }
            ctx.status(204);
        }, ADMINISTRADOR, SISTEMA);

        app.put("/api/rutas/{id}/estado", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            validarChoferDeRuta(ctx, id);
            String estado = ctx.body() != null ? ctx.body().trim() : "";
            if (estado.startsWith("\"") && estado.endsWith("\"") && estado.length() >= 2) {
                estado = estado.substring(1, estado.length() - 1);
            }
            rutaService.actualizarEstado(id, estado);
            ctx.status(204);
        }, CHOFER, ADMINISTRADOR, SISTEMA);

        app.post("/api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones", ctx -> {
            Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
            Long paradaId = Long.parseLong(ctx.pathParam("paradaId"));
            validarChoferDeRuta(ctx, rutaId);
            rutaService.confirmarEntregaExitosa(rutaId, paradaId);
            ctx.status(204);
        }, CHOFER, ADMINISTRADOR, SISTEMA);

        app.post("/api/rutas/{rutaId}/paradas/{paradaId}/fallos", ctx -> {
            Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
            Long paradaId = Long.parseLong(ctx.pathParam("paradaId"));
            validarChoferDeRuta(ctx, rutaId);
            String motivo = leerMotivoOpcional(ctx);
            rutaService.registrarEntregaNoRecibida(rutaId, paradaId, motivo);
            ctx.status(204);
        }, CHOFER, ADMINISTRADOR, SISTEMA);

        app.get("/api/rutas/{id}/ubicacion", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(rutaService.obtenerUbicacionActual(id));
        });

        app.post("/api/rutas/planificaciones", ctx -> {
            planificadorDeRutas.planificarRutas();
            ctx.status(204);
        }, ADMINISTRADOR, SISTEMA);

        app.post("/api/rutas/planificaciones/resultados", ctx -> {
            EjecutarPlanificacionRequest request = ctx.bodyAsClass(EjecutarPlanificacionRequest.class);
            planificadorDeRutas.ejecutarPlanificacion(request);
            ctx.status(204);
        }, SISTEMA, ADMINISTRADOR);
    }

    private void validarChoferDeRuta(Context ctx, Long rutaId) {
        if (!Autorizacion.esPrivilegiado(ctx)) {
            RutaResponse ruta = rutaService.obtenerPorId(rutaId);
            Autorizacion.exigirPropietario(ctx, CHOFER, ruta.camionId());
        }
    }

    @SuppressWarnings("unchecked")
    private String leerMotivoOpcional(Context ctx) {
        String body = ctx.body();
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> json = ctx.bodyAsClass(Map.class);
            Object valor = json != null ? json.get("motivo") : null;
            return valor != null ? valor.toString() : null;
        } catch (Exception ignored) {
            return body.trim();
        }
    }
}
