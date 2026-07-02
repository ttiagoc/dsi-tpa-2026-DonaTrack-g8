package ar.edu.utn.frba.ddsi.logistica.services;

import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;

public interface PlanificacionRutasService {

    void planificarRutas();

    void ejecutarPlanificacion(EjecutarPlanificacionRequest request);
}
