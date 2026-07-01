package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDate;

public record BienInfo(
    String descripcion,
    Long cantidad,
    Double pesoKgPorUnidad,
    Double volumenM3PorUnidad,
    String subcategoria,
    String estadoBien,
    LocalDate fechaVencimiento
) {}
