package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

public record ActualizarPersonaJuridicaResponse(
    Long id,
    String razonSocial,
    String cuit
) {}
