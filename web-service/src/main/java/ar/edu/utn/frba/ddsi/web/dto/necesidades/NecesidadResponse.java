package ar.edu.utn.frba.ddsi.web.dto.necesidades;

/**
 * Lo que devuelve GET /entidad-beneficiaria/{id}/necesidades.
 * tipoNecesidad llega como el nombre de la clase ("NecesidadRecurrente" o "NecesidadExtraordinaria").
 * donacionesEnCurso: asignadas que todavia no llegaron; mientras haya alguna, la API no deja eliminarla.
 * unidad: en que se miden cantidad y cantidadAsignada (UNIDADES, KILOGRAMOS o LITROS).
 * cantidadAsignada puede tener decimales (5 paquetes de 0,5 kg = 2,5 kg).
 */
public record NecesidadResponse(
        Long id,
        String subcategoria,
        String tipoNecesidad,
        String periodo,
        String descripcion,
        Long cantidad,
        String categoria,
        Double cantidadAsignada,
        Boolean satisfecha,
        Long donacionesEnCurso,
        String unidad) {
}
