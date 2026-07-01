package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDateTime;
import java.util.List;

public record DonacionResponse(
                Long id,
                List<BienResponse> bienes,
                String estadoActual,
                LocalDateTime fecha,
                Long donanteId,
                Long entidadAsignadaId) {
}
