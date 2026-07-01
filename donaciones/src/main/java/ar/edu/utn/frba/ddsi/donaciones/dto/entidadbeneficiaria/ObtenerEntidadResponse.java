package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import java.util.List;

public record ObtenerEntidadResponse(
    Long id,
    String razonSocial,
    String direccion,
    String telefono,
    List<String> correoRepresentantes,
    List<NecesidadInfo> necesidades
) {}
