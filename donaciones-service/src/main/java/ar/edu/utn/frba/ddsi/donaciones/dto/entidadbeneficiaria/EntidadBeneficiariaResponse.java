package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import java.util.List;

public record EntidadBeneficiariaResponse(
                Long id,
                String razonSocial,
                String direccion,
                String telefono,
                List<String> correos,
                List<NecesidadResponse> necesidades) {
}
