package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada;

public record ParadaResponse(
    Long id,
    Integer orden,
    String destino,
    Long entidad,
    List<Long> entregas,
    EstadoParada estado
) {
    public ParadaResponse(Long id, Integer orden, String destino, Long entidad, List<Long> entregas) {
        this(id, orden, destino, entidad, entregas, EstadoParada.PENDIENTE);
    }
}
