package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.PlanificadorDeRutas;

@RestController
@RequestMapping("/api/logistica-service/planificacion")
public class PlanificacionRutasController {

    private final PlanificadorDeRutas planificadorDeRutas;

    public PlanificacionRutasController(PlanificadorDeRutas planificadorDeRutas) {
        this.planificadorDeRutas = planificadorDeRutas;
    }

    @PostMapping("/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ejecutarPlanificacion(@RequestBody EjecutarPlanificacionRequest request) {
        planificadorDeRutas.ejecutarPlanificacion(request);
    }
}
