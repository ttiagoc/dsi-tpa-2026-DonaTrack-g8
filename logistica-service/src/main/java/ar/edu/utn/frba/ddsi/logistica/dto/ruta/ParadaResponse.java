package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.util.List;

public record ParadaResponse(
    Long id,
    Integer orden,
    String destino,
    Long entidad,
    List<Long> entregas
) {}
