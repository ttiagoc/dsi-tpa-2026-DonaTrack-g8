package ar.edu.utn.frba.ddsi.web.dto.necesidades;

/**
 * Lo que devuelve GET /entidad-beneficiaria/{id}/necesidades.
 * tipoNecesidad llega como el nombre de la clase ("NecesidadRecurrente" o "NecesidadExtraordinaria").
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
        Boolean satisfecha) {
}
