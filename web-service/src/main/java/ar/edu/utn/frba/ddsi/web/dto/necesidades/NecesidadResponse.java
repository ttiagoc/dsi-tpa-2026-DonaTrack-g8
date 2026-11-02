package ar.edu.utn.frba.ddsi.web.dto.necesidades;

/**
 * Lo que devuelve GET /entidad-beneficiaria/{id}/necesidades.
 * tipoNecesidad llega como el nombre de la clase ("NecesidadRecurrente" o "NecesidadExtraordinaria").
 * donacionesEnCurso: asignadas que todavia no llegaron; mientras haya alguna, la API no deja eliminarla.
 */
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
        Long donacionesEnCurso) {
}
