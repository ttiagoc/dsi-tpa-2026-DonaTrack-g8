package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

public record DonacionAsignadaInfo(
    Long id,
    Double peso,
    Double volumen,
    String direccion
) {}
