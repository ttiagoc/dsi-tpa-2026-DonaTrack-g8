package ar.edu.utn.frba.ddsi.donaciones.dto.evento;

import java.util.List;

public record ParadaInfo(
    Long entidadId,
    List<Long> donacionIds
) {}
