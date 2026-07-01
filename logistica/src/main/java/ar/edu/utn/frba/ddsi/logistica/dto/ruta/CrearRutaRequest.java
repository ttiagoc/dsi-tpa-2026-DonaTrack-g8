package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDate;

public record CrearRutaRequest(
    LocalDate fecha,
    Long camionId,
    Long choferId
) {}
