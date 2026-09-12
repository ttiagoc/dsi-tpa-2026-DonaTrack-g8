package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;

public record NecesidadRequest(
                SubcategoriaRequest subcategoria,
                String tipoNecesidad,
                String periodo,
                String descripcion,
                Long cantidad) {
}
