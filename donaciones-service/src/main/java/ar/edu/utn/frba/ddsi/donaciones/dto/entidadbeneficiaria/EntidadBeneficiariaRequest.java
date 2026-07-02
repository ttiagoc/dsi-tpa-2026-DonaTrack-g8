package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;

public record EntidadBeneficiariaRequest(
        String razonSocial,
        String direccion,
        String telefono,
        List<MedioContactoRequest> correoRepresentantes) {
}
