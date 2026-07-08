package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDate;
import java.util.List;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;

public record RutaResponse(
    Long id,
    LocalDate fecha,
    EstadoRuta estado,
    String patenteCamion,
    List<ParadaResponse> paradas
) {}