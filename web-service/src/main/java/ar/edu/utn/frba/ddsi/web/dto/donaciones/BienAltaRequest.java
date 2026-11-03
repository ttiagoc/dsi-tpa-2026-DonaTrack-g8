package ar.edu.utn.frba.ddsi.web.dto.donaciones;

import java.time.LocalDate;

public record BienAltaRequest(
        String descripcion,
        Long cantidad,
        Double pesoKgPorUnidad,
        Double volumenM3PorUnidad,
        String estado,
        LocalDate fechaVencimiento,
        SubcategoriaAltaRequest subcategoria
) {
}

