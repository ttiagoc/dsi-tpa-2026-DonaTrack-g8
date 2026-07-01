package ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones;

import java.util.List;

public record InicioRutaRequest(
    Long rutaId,
    List<ParadaInfo> paradas
) {}
