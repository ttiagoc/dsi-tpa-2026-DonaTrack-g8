package ar.edu.utn.frba.ddsi.donaciones.dto.evento;

import java.util.List;

public record InicioRutaRequest(
        Long rutaId,
        List<ParadaRequest> paradas) {
}
