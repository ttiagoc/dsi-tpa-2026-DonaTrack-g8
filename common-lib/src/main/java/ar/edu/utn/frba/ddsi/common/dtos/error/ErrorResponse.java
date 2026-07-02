package ar.edu.utn.frba.ddsi.common.dtos.error;

import java.time.Instant;

public record ErrorResponse(
    String error,
    String message,
    Instant timestamp
) {}
