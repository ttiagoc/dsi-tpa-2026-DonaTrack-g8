package ar.edu.utn.frba.ddsi.web.dto.necesidades;

/** Body de POST /entidad-beneficiaria/{id}/necesidades. */
public record NecesidadRequest(
        Subcategoria subcategoria,
        String tipoNecesidad,
        String periodo,
        String descripcion,
        Long cantidad,
        String unidad) {

    public record Subcategoria(String nombre) {
    }
}
