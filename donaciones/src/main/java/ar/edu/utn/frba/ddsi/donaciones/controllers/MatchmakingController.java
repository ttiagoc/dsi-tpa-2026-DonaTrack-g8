package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.MatchmakingService;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matchmaking")
@AllArgsConstructor
public class MatchmakingController {

    private final ResultadoMatchmakingRepository resultadoRepository;
    private final MatchmakingService matchmakingService;

    /**
     * Trae todas las propuestas calculadas a la madrugada que esperan revisión.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<ResultadoMatchmaking>> obtenerPropuestasPendientes() {
        List<ResultadoMatchmaking> pendientes = resultadoRepository.buscarPendientes();
        return ResponseEntity.ok(pendientes);
    }

    /**
     * Confirma el destino de la donación.
     */
    @PostMapping("/propuestas/{id}/aceptar")
    public ResponseEntity<String> aceptarPropuesta(@PathVariable Long id, @RequestParam Long entidadId) {
        try {
            matchmakingService.aceptarPropuesta(id, entidadId);
            return ResponseEntity.ok("Donación asignada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Rechaza las sugerencias actuales.
     */
    @PostMapping("/propuestas/{id}/rechazar")
    public ResponseEntity<String> rechazarPropuesta(@PathVariable Long id) {
        try {
            matchmakingService.rechazarPropuesta(id);
            return ResponseEntity.ok("Propuesta rechazada con éxito.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Forzar el proceso de matchmaking por HTTP sin esperar a las 3 AM.
     */
    @PostMapping("/forzar-ejecucion")
    public ResponseEntity<String> forzarEjecucion() {
        matchmakingService.procesarMatchmakingGlobal();
        return ResponseEntity.ok("Proceso de matchmaking ejecutado manualmente con éxito.");
    }
}