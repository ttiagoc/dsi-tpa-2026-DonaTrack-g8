package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record CamionPlanificacionInfo(
    Long id,
    List<DireccionInfo> direcciones
) {}
