package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDate;

public record BienRequest(String descripcion, Long cantidad, Double pesoKgPorUnidad, Double volumenM3PorUnidad,
        String estado, LocalDate fechaVencimiento, SubcategoriaRequest subcategoria) {
}
