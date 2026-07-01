package ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones;

import java.util.List;

public record ParadaInfo(
    Long entidadId,
    List<Long> donacionIds
) {}
