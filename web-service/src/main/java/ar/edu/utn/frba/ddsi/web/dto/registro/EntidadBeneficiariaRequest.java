package ar.edu.utn.frba.ddsi.web.dto.registro;

import java.util.List;

public record EntidadBeneficiariaRequest(
        String razonSocial,
        String direccion,
        String telefono,
        List<MedioContactoRequest> correoRepresentantes) {
}
