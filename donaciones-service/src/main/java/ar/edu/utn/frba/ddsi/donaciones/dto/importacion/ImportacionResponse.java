package ar.edu.utn.frba.ddsi.donaciones.dto.importacion;

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
) {}

