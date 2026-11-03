package ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking;

import java.time.LocalDateTime;
import java.util.List;

public record PropuestaMatchmakingResponse(
        Long id,
        Long donacionId,
        String subcategoria,
        String categoria,
        String bienesResumen,
        Long totalUnidades,
        List<Long> entidadesSugeridasIds,
        List<EntidadSugeridaDto> entidadesSugeridas,
        LocalDateTime fechaEjecucion
) {
    public record EntidadSugeridaDto(
            Long id,
            String razonSocial,
            String direccion,
            int orden
    ) {}
}
