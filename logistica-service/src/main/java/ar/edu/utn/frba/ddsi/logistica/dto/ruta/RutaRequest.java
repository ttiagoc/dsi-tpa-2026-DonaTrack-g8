package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDate;
import java.util.List;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;

public record RutaRequest(
    LocalDate fecha,
    EstadoRuta estado,
    String patenteCamion,
    List<ParadaRequest> paradas
) {}
