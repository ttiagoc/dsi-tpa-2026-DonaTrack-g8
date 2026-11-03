package ar.edu.utn.frba.ddsi.web.dto.matchmaking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    ) {
        public boolean esPrioridadUno() {
            return orden == 1;
        }
    }

    public String fechaFormateada() {
        if (fechaEjecucion == null) return "-";
        return fechaEjecucion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public int cantidadSugerencias() {
        return entidadesSugeridas != null ? entidadesSugeridas.size() : (entidadesSugeridasIds != null ? entidadesSugeridasIds.size() : 0);
    }
}

