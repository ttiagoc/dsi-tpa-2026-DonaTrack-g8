package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.AceptarPropuestaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ForzarEjecucionMatchmakingResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ObtenerPropuestasPendientesResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.RechazarPropuestaResponse;
import ar.edu.utn.frba.ddsi.donaciones.services.MatchmakingService;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donaciones/matchmaking")
@AllArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @GetMapping("/pendientes")
    public ObtenerPropuestasPendientesResponse obtenerPropuestasPendientes() {
        return matchmakingService.obtenerPropuestasPendientes();
    }

    @PostMapping("/propuestas/{id}/aceptar")
    @ResponseStatus(HttpStatus.CREATED)
    public AceptarPropuestaResponse aceptarPropuesta(@PathVariable Long id, @RequestParam Long entidadId) {
        matchmakingService.aceptarPropuesta(id, entidadId);
        return new AceptarPropuestaResponse("DonaciÃ³n asignada correctamente.");
    }

    @PostMapping("/propuestas/{id}/rechazar")
    @ResponseStatus(HttpStatus.CREATED)
    public RechazarPropuestaResponse rechazarPropuesta(@PathVariable Long id) {
        matchmakingService.rechazarPropuesta(id);
        return new RechazarPropuestaResponse("Propuesta rechazada con Ã©xito.");
    }

    @PostMapping("/forzar-ejecucion")
    @ResponseStatus(HttpStatus.CREATED)
    public ForzarEjecucionMatchmakingResponse forzarEjecucion() {
        matchmakingService.procesarMatchmakingGlobal();
        return new ForzarEjecucionMatchmakingResponse("Proceso de matchmaking ejecutado manualmente con Ã©xito.");
    }
}
