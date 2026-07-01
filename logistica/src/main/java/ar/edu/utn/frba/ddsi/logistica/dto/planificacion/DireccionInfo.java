package ar.edu.utn.frba.ddsi.logistica.dto.planificacion;

import java.util.List;

public record DireccionInfo(
    String direccion,
    List<Long> donacionesIds
) {}
