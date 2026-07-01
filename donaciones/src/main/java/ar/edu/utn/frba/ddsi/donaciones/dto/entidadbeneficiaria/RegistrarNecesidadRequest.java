package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

public record RegistrarNecesidadRequest(
    String subcategoria,
    String tipoNecesidad,
    String descripcion,
    Long cantidad
) {}
