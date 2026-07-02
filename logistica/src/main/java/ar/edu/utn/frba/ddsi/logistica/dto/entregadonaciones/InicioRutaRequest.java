package ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones;

import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.ruta.ParadaRequest;

public record InicioRutaRequest(
                Long rutaId,
                List<ParadaRequest> paradas) {
}
