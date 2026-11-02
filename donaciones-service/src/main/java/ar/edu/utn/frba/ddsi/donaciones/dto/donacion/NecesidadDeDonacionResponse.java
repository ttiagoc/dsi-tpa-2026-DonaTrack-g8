package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

// la necesidad que cubre una donacion asignada
public record NecesidadDeDonacionResponse(
        Long id,
        String subcategoria,
        String descripcion) {
}
