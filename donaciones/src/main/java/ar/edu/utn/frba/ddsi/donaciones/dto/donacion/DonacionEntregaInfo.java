package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

public record DonacionEntregaInfo(
    Long id,
    Double peso,
    Double volumen,
    String direccion
) {}
