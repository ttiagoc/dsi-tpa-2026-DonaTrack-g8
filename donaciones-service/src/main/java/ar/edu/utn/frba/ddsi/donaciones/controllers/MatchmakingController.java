package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.SISTEMA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.EstadoPropuestaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.MotorDeMatchmaking;
import io.javalin.Javalin;

@Component
public class MatchmakingController implements JavalinController {

    private final MotorDeMatchmaking motorDeMatchmaking;

    public MatchmakingController(MotorDeMatchmaking motorDeMatchmaking) {
        this.motorDeMatchmaking = motorDeMatchmaking;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/matchmaking/pendientes", ctx -> {
            ctx.json(motorDeMatchmaking.obtenerPropuestasPendientes());
        }, ADMINISTRADOR);

        app.put("/api/matchmaking/propuestas/{id}/estado", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            EstadoPropuestaRequest request = ctx.bodyAsClass(EstadoPropuestaRequest.class);
            motorDeMatchmaking.actualizarEstadoPropuesta(id, request);
            ctx.status(204);
        }, ADMINISTRADOR);

        // a demanda (el administrador) o todas las noches desde el cron de la VM con la clave de servicio
        app.post("/api/matchmaking/ejecuciones", ctx -> {
            motorDeMatchmaking.procesarMatchmaking();
            ctx.status(204);
        }, ADMINISTRADOR, SISTEMA);
    }
}
