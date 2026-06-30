package ar.edu.utn.frba.ddsi.logistica.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.logistica.dto.ResultadoPlanificacionDTO;
import ar.edu.utn.frba.ddsi.logistica.services.PlanificacionRutasService;

@RestController
@RequestMapping("/api/planificacion")
public class PlanificacionRutasController {

    private final PlanificacionRutasService planificacionRutasService;

    public PlanificacionRutasController(PlanificacionRutasService planificacionRutasService) {
        this.planificacionRutasService = planificacionRutasService;
    }

    @PostMapping("/confirmacion")
    public ResponseEntity<Void> ejecutarPlanificacion(
            @RequestBody ResultadoPlanificacionDTO resultadoPlanificacion) {
        try {
            planificacionRutasService.ejecutarPlanificacion(resultadoPlanificacion);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
