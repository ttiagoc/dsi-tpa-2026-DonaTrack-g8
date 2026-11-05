package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.time.Instant;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.dtos.error.ErrorResponse;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Geocodificador;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.GeocodificadorNoDisponibleException;
import io.javalin.Javalin;

/**
 * Publico: ubica una direccion antes de registrar la entidad, para mostrarla en el mapa del formulario.
 * Usa el mismo geocodificador que el alta, asi lo que se ve en el mapa es lo que despues se guarda.
 * 400 si la direccion no existe o esta incompleta; 503 si Georef no responde.
 */
@Component
public class DireccionController implements JavalinController {

    private final Geocodificador geocodificador;

    public DireccionController(Geocodificador geocodificador) {
        this.geocodificador = geocodificador;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/direcciones/ubicar", ctx -> {
            String direccion = ctx.queryParam("direccion");
            try {
                ctx.json(geocodificador.ubicar(direccion == null ? "" : direccion));
            } catch (GeocodificadorNoDisponibleException e) {
                ctx.status(503).json(new ErrorResponse("service_unavailable",
                        "No pudimos buscar la dirección ahora. Se va a verificar al registrar.", Instant.now()));
            }
        });
    }
}
