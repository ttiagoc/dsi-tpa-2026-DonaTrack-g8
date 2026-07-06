package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record CamionPlanificacionRequest(
                Long id,
                List<DireccionRequest> direcciones) {
}
