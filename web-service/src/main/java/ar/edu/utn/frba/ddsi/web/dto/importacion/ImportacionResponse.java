package ar.edu.utn.frba.ddsi.web.dto.importacion;

import java.util.List;

public record ImportacionResponse(
        String id,
        String nombreArchivo,
        String fecha,
        String estado,
        int totalFilas,
        int procesadas,
        int creados,
        int actualizados,
        int descartadas,
        int porcentaje,
        List<FilaErrorDto> errores
) {
    public boolean esEnProceso() {
        return "EN_PROCESO".equalsIgnoreCase(estado);
    }

    public boolean esCompletada() {
        return "COMPLETADA".equalsIgnoreCase(estado);
    }

    public boolean esFallida() {
        return "FALLIDA".equalsIgnoreCase(estado);
    }

    public boolean tieneErrores() {
        return errores != null && !errores.isEmpty();
    }
}

