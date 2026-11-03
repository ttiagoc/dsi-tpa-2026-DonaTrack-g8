package ar.edu.utn.frba.ddsi.donaciones.dto.gamificacion;

public record MisionProgresoResponse (
    String codigo,
    String nombre,
    String descripcion,
    int progresoActual,
    int meta,
    boolean cumplida,
    int porcentaje,
    InsigniaResponse insignia
) {}
