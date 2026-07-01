package ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones;

import java.time.LocalDateTime;
import java.util.List;

public record ConfirmacionEntregaExitosaRequest(
    Long entidadId,
    List<Long> donacionIds,
    String patenteCamion,
    LocalDateTime fechaHora
) {}
