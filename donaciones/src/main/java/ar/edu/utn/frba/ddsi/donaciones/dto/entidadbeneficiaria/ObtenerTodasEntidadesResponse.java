package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

public record ObtenerTodasEntidadesResponse(
    Long id,
    String razonSocial,
    String direccion
) {}
