package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record DireccionRequest(
        String direccion,
        List<Long> donacionesIds,
        Long entidadId) {

    public DireccionRequest(String direccion, List<Long> donacionesIds) {
        this(direccion, donacionesIds, null);
    }
}
