package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaMatchmakingResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.MotorDeMatchmaking;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/donaciones-service/matchmaking")
@AllArgsConstructor
public class MatchmakingController {

    private final MotorDeMatchmaking motorDeMatchmaking;

    @GetMapping("/pendientes")
    public List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes() {
        return motorDeMatchmaking.obtenerPropuestasPendientes();
    }

    @PostMapping("/propuestas/{id}/aceptar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceptarPropuesta(@PathVariable Long id, @RequestParam Long entidadId) {
        motorDeMatchmaking.aceptarPropuesta(id, entidadId);
    }

    @PostMapping("/propuestas/{id}/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazarPropuesta(@PathVariable Long id) {
        motorDeMatchmaking.rechazarPropuesta(id);
    }

    @PostMapping("/forzar-ejecucion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forzarEjecucion() {
        motorDeMatchmaking.procesarMatchmaking();
    }
}
