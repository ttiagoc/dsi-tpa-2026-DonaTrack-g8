package ar.edu.utn.frba.ddsi.logistica.dto.monitoreo;

import java.util.List;

public record CamionActivoResponse(
    Long camionId,
    String patente,
    Double latitud,
    Double longitud,
    Double velocidad,
    List<ParadaPendienteResponse> paradasPendientes
) {}