package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record EjecutarPlanificacionRequest(
    List<CamionPlanificacionInfo> camiones,
    List<Long> donacionesSinAsignar
) {}
