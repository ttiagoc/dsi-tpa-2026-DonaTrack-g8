package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

public record BienResponse(
                String descripcion,
                Long cantidad,
                Double pesoKgPorUnidad,
                Double volumenM3PorUnidad) {
}
