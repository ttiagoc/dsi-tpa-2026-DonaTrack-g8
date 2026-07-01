package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDateTime;

public record EstadoDonacionResponse(
        Long id,
        String estadoActual,
        LocalDateTime fechaCambio) {
}
