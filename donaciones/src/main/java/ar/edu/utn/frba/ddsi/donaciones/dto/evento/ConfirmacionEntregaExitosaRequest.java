package ar.edu.utn.frba.ddsi.donaciones.dto.evento;

import java.time.LocalDateTime;
import java.util.List;

public record ConfirmacionEntregaExitosaRequest(
    Long entidadId,
    List<Long> donacionIds,
    String patenteCamion,
    LocalDateTime fechaHora
) {}
