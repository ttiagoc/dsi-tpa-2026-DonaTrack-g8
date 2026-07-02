package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

public record NecesidadResponse(
        Long id,
        String subcategoria,
        String tipoNecesidad,
        String descripcion,
        Long cantidad) {
}
