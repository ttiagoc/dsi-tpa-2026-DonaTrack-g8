package ar.edu.utn.frba.ddsi.logistica.dto.monitoreo;

import java.time.LocalDateTime;

public record RecibirTelemetriaRequest(
    Double latitud,
    Double longitud,
    LocalDateTime timestamp,
    Double velocidad
) {}
