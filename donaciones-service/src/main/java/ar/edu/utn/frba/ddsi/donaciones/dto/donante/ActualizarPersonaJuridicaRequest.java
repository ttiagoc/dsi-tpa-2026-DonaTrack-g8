package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

public record ActualizarPersonaJuridicaRequest(
    String razonSocial,
    String rubro,
    String tipo,
    String cuit
) {}
