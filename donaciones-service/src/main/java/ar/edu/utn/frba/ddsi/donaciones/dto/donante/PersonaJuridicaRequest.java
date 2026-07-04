package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

import java.util.List;

public record PersonaJuridicaRequest(
        String razonSocial,
        String rubro,
        String tipo,
        String cuit,
        List<RepresentanteRequest> representantes,
        List<MedioContactoRequest> contactos,
        MedioContactoRequest contactoPredeterminado) {
}
