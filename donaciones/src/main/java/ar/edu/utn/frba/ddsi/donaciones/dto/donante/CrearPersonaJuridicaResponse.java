package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

public record CrearPersonaJuridicaResponse(
    Long id,
    String razonSocial,
    String cuit
) {}
