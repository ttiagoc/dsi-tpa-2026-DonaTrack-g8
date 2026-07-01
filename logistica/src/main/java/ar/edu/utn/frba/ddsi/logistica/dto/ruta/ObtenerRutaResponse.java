package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDate;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;

public record ObtenerRutaResponse(
    Long id,
    LocalDate fecha,
    EstadoRuta estado,
    String patenteCamion
) {}
