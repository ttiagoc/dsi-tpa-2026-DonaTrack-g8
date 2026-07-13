package ar.edu.utn.frba.ddsi.donaciones.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.EstadoPropuestaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaMatchmakingResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.MotorDeMatchmaking;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/matchmaking")
@AllArgsConstructor
public class MatchmakingController {

    private final MotorDeMatchmaking motorDeMatchmaking;

    @GetMapping("/pendientes")
    public List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes() {
        return motorDeMatchmaking.obtenerPropuestasPendientes();
    }

    @PutMapping("/propuestas/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarEstadoPropuesta(@PathVariable Long id, @RequestBody EstadoPropuestaRequest request) {
        motorDeMatchmaking.actualizarEstadoPropuesta(id, request);
    }

    @PostMapping("/ejecuciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ejecutarMatchmaking() {
        motorDeMatchmaking.procesarMatchmaking();
    }
}
