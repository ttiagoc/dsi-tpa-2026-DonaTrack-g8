package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

public record NecesidadInfo(
    Long id,
    String subcategoria,
    String tipoNecesidad,
    String descripcion,
    Long cantidad
) {}
