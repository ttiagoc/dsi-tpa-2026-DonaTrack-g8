package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDate;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;

public record ActualizarRutaRequest(
    LocalDate fecha,
    EstadoRuta estado,
    Long camionId,
    Long choferId
) {}
