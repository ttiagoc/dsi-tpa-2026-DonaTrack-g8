package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDate;
import java.util.List;

public record RutaRequest(
                LocalDate fecha,
                Long idCamion,
                List<ParadaRequest> paradas) {
}
