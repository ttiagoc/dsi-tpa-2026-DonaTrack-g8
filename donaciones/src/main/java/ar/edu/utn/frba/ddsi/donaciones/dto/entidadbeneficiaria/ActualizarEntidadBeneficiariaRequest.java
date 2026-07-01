package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import java.util.List;

public record ActualizarEntidadBeneficiariaRequest(
    String razonSocial,
    String direccion,
    String telefono,
    List<String> correoRepresentantes
) {}
