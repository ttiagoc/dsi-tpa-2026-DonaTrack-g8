package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.time.LocalDateTime;
import java.util.List;

public record RutaSeguimientoResponse(
    Long rutaId,
    String patenteCamion,
    String chofer,
    Double latitud,
    Double longitud,
    Double velocidad,
    LocalDateTime ultimaActualizacion,
    List<ParadaResponse> paradas
) {}
