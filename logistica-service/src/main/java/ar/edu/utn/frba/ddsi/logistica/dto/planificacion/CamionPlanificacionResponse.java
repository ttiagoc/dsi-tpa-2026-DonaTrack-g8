package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record CamionPlanificacionResponse(
    Long id,
    List<DireccionResponse> direcciones
) {}
