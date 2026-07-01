package ar.edu.utn.frba.ddsi.logistica.dto.monitoreo;

import java.util.List;

public record ObtenerCamionesActivosResponse(List<CamionActivoInfo> camiones) {}