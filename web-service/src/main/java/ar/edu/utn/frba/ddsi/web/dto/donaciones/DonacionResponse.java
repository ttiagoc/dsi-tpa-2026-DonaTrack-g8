package ar.edu.utn.frba.ddsi.web.dto.donaciones;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que devuelve GET /donaciones?entidadId=X. estadoActual llega como el nombre del enum (EN_TRASLADO, ENTREGADA...).
 * necesidad es null si la necesidad se elimino despues de que llego la donacion.
 */
public record DonacionResponse(
        Long id,
        List<Bien> bienes,
        String estadoActual,
        LocalDateTime fecha,
        String subcategoria,
        String categoria,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaEstado,
        Necesidad necesidad,
        List<String> fotosRecepcion) {

    public record Bien(String descripcion, Long cantidad) {
    }

    public record Necesidad(Long id, String subcategoria, String descripcion) {
    }
}
