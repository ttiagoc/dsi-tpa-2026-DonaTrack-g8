package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDateTime;
import java.util.List;

public record ObtenerDonacionResponse(
    Long id,
    String subcategoria,
    String estadoBienes,
    List<BienInfo> bienes,
    String estadoActual,
    LocalDateTime fecha,
    Long donanteId,
    Long entidadAsignadaId
) {}
