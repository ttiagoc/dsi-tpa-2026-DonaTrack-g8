package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record DireccionRequest(
        String direccion,
        List<Long> donacionesIds,
        Long entidadId,
        Double latitud,
        Double longitud) {

    public DireccionRequest(String direccion, List<Long> donacionesIds) {
        this(direccion, donacionesIds, null);
    }

    public DireccionRequest(String direccion, List<Long> donacionesIds, Long entidadId) {
        this(direccion, donacionesIds, entidadId, null, null);
    }
}
