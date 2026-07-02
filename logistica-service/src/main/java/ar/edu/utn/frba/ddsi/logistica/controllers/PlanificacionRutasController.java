package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.services.PlanificacionRutasService;

@RestController
@RequestMapping("/api/logistica/planificacion")
public class PlanificacionRutasController {

    private final PlanificacionRutasService planificacionRutasService;

    public PlanificacionRutasController(PlanificacionRutasService planificacionRutasService) {
        this.planificacionRutasService = planificacionRutasService;
    }

    @PostMapping("/confirmacion")
    @ResponseStatus(HttpStatus.CREATED)
    public void ejecutarPlanificacion(
        @RequestBody EjecutarPlanificacionRequest request) {
        planificacionRutasService.ejecutarPlanificacion(request);
        return;
    }
}
