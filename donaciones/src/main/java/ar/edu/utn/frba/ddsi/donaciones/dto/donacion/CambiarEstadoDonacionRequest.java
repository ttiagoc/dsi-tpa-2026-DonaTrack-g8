package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

public record CambiarEstadoDonacionRequest(
    String estado,
    String justificacion
) {}
