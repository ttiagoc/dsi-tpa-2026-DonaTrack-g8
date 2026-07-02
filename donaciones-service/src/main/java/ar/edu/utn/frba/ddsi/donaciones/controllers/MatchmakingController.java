package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.AceptarPropuestaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ForzarEjecucionMatchmakingResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ObtenerPropuestasPendientesResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.RechazarPropuestaResponse;
import ar.edu.utn.frba.ddsi.donaciones.services.MatchmakingService;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donaciones/matchmaking")
@AllArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @GetMapping("/pendientes")
    public ResponseEntity<ObtenerPropuestasPendientesResponse> obtenerPropuestasPendientes() {
        return ResponseEntity.ok(matchmakingService.obtenerPropuestasPendientes());
    }

    @PostMapping("/propuestas/{id}/aceptar")
    public ResponseEntity<AceptarPropuestaResponse> aceptarPropuesta(@PathVariable Long id, @RequestParam Long entidadId) {
        matchmakingService.aceptarPropuesta(id, entidadId);
        return ResponseEntity.ok(new AceptarPropuestaResponse("Donación asignada correctamente."));
    }

    @PostMapping("/propuestas/{id}/rechazar")
    public ResponseEntity<RechazarPropuestaResponse> rechazarPropuesta(@PathVariable Long id) {
        matchmakingService.rechazarPropuesta(id);
        return ResponseEntity.ok(new RechazarPropuestaResponse("Propuesta rechazada con éxito."));
    }

    @PostMapping("/forzar-ejecucion")
    public ResponseEntity<ForzarEjecucionMatchmakingResponse> forzarEjecucion() {
        matchmakingService.procesarMatchmakingGlobal();
        return ResponseEntity.ok(new ForzarEjecucionMatchmakingResponse("Proceso de matchmaking ejecutado manualmente con éxito."));
    }
}