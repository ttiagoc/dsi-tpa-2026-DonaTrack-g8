package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

import java.util.List;

public record CrearPersonaJuridicaRequest(
    String razonSocial,
    String rubro,
    String tipo,
    String cuit,
    List<RepresentanteInfo> representantes
) {}
