package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

// cantidadAsignada: bienes de las donaciones ya asignadas (en las recurrentes, solo del periodo actual)
// unidad: en que se miden cantidad y cantidadAsignada (UNIDADES, KILOGRAMOS o LITROS)
// donacionesEnCurso: asignadas que todavia no llegaron; mientras haya alguna, la necesidad no se puede eliminar
public record NecesidadResponse(
        Long id,
        String subcategoria,
        String tipoNecesidad,
        String periodo,
        String descripcion,
        Long cantidad,
        String categoria,
        Long cantidadAsignada,
        Boolean satisfecha,
        Long donacionesEnCurso,
        String unidad) {
}
