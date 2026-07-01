package ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking;

import java.time.LocalDateTime;
import java.util.List;

public record PropuestaMatchmakingInfo(
    Long id,
    Long donacionId,
    List<Long> entidadesSugeridasIds,
    LocalDateTime fechaEjecucion,
    String estado
) {}
