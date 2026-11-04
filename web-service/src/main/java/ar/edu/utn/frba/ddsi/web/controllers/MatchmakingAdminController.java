package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.matchmaking.PropuestaMatchmakingResponse;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

@Component
public class MatchmakingAdminController {

    private static final String RUTA = "/admin/matchmaking";
    private final DonacionesApiClient donaciones;

    public MatchmakingAdminController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        Map<String, Object> modelo = Modelo.base(ctx);
        List<PropuestaMatchmakingResponse> propuestas = donaciones.obtenerPropuestasPendientes(ctx);

        long candidatas = propuestas != null
                ? propuestas.stream()
                        .filter(p -> p.entidadesSugeridas() != null)
                        .flatMap(p -> p.entidadesSugeridas().stream())
                        .map(PropuestaMatchmakingResponse.EntidadSugeridaDto::id)
                        .distinct()
                        .count()
                : 0;

        long totalSugerencias = propuestas != null
                ? propuestas.stream().mapToInt(PropuestaMatchmakingResponse::cantidadSugerencias).sum()
                : 0;

        modelo.put("propuestas", propuestas);
        modelo.put("resumen", Map.of(
                "pendientes", propuestas != null ? propuestas.size() : 0,
                "candidatas", candidatas,
                "totalSugerencias", totalSugerencias
        ));

        String propuestaParam = ctx.queryParam("propuesta");
        if (propuestaParam != null && !propuestaParam.isBlank()) {
            try {
                Long propuestaId = Long.parseLong(propuestaParam);
                propuestas.stream()
                        .filter(p -> p.id().equals(propuestaId))
                        .findFirst()
                        .ifPresent(p -> {
                            modelo.put("abrirModalSugerencias", true);
                            modelo.put("propuestaSeleccionada", p);
                        });
            } catch (Exception ignored) {
            }
        }

        ctx.render("admin/matchmaking.html.hbs", modelo);
    }

    public void ejecutar(Context ctx) {
        try {
            donaciones.ejecutarMatchmaking(ctx);
            Alertas.exito(ctx, "El matchmaking se ejecutó exitosamente y se generaron nuevas propuestas.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo ejecutar el matchmaking: " + e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    public void aceptar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        String entidadIdStr = ctx.formParam("entidadId");

        if (entidadIdStr == null || entidadIdStr.isBlank()) {
            Alertas.error(ctx, "Debés seleccionar una entidad para asignar la donación.");
            ctx.redirect(RUTA + "?propuesta=" + id);
            return;
        }

        try {
            Long entidadId = Long.parseLong(entidadIdStr);
            donaciones.aceptarPropuesta(ctx, id, entidadId);
            Alertas.exito(ctx, "La donación fue asignada exitosamente a la entidad elegida.");
            ctx.redirect(RUTA);
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo asignar la donación: " + e.getMessage());
            ctx.redirect(RUTA + "?propuesta=" + id);
        }
    }

    public void rechazar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.rechazarPropuesta(ctx, id);
            Alertas.info(ctx, "La propuesta de matchmaking #" + id + " fue descartada.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo descartar la propuesta: " + e.getMessage());
        }
        ctx.redirect(RUTA);
    }
}

