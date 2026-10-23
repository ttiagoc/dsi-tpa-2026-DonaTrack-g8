package ar.edu.utn.frba.ddsi.web.dto.registro;

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
