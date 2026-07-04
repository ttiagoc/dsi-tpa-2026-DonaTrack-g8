package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaMatchmakingResponse;
import ar.edu.utn.frba.ddsi.donaciones.services.MatchmakingService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/donaciones-service/matchmaking")
@AllArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @GetMapping("/pendientes")
    public List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes() {
        return matchmakingService.obtenerPropuestasPendientes();
    }

    @PostMapping("/propuestas/{id}/aceptar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceptarPropuesta(@PathVariable Long id, @RequestParam Long entidadId) {
        matchmakingService.aceptarPropuesta(id, entidadId);
    }

    @PostMapping("/propuestas/{id}/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazarPropuesta(@PathVariable Long id) {
        matchmakingService.rechazarPropuesta(id);
    }

    @PostMapping("/forzar-ejecucion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forzarEjecucion() {
        matchmakingService.procesarMatchmaking();
    }
}
