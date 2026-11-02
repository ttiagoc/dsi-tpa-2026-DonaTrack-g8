package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;

// unidad: UNIDADES, KILOGRAMOS o LITROS; si no viene, UNIDADES
public record NecesidadRequest(
                SubcategoriaRequest subcategoria,
                String tipoNecesidad,
                String periodo,
                String descripcion,
                Long cantidad,
                String unidad) {
}
