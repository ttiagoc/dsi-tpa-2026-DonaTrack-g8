package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

public record RegistrarNecesidadResponse(
    Long id,
    String descripcion,
    Long cantidad,
    String tipoNecesidad
) {}
