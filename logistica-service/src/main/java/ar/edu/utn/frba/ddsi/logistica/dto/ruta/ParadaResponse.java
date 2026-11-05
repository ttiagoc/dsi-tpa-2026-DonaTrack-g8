package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada;
import java.util.List;

public record ParadaResponse(
    Long id,
    Integer orden,
    String destino,
    Long entidad,
    List<Long> entregas,
    EstadoParada estado,
    Double latitud,
    Double longitud
) {
    public ParadaResponse(Long id, Integer orden, String destino, Long entidad, List<Long> entregas, EstadoParada estado) {
        this(id, orden, destino, entidad, entregas, estado, null, null);
    }

    public ParadaResponse(Long id, Integer orden, String destino, Long entidad, List<Long> entregas) {
        this(id, orden, destino, entidad, entregas, EstadoParada.PENDIENTE, null, null);
    }
}
