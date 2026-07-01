package ar.edu.utn.frba.ddsi.logistica.dto.camion;

import java.util.List;

public record ObtenerTodosCamionesResponse(
    List<ObtenerCamionResponse> camiones
) {}
