package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDateTime;

public record ObtenerTodasDonacionesResponse(
    Long id,
    String subcategoria,
    String estadoActual,
    LocalDateTime fecha
) {}
