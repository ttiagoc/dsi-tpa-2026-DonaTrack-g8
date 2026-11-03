package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDateTime;
import java.util.List;

// fechaAsignacion: la ultima vez que se asigno a una entidad. fechaEstado: cuando paso al estado actual.
// necesidad: null si no esta asignada o si la necesidad se elimino
public record DonacionResponse(
                Long id,
                List<BienResponse> bienes,
                String estadoActual,
                LocalDateTime fecha,
                Long donanteId,
                Long entidadAsignadaId,
                String subcategoria,
                String categoria,
                LocalDateTime fechaAsignacion,
                LocalDateTime fechaEstado,
                NecesidadDeDonacionResponse necesidad,
                List<String> fotosRecepcion,
                String justificacion) {

    public DonacionResponse(Long id, List<BienResponse> bienes, String estadoActual, LocalDateTime fecha,
            Long donanteId, Long entidadAsignadaId, String subcategoria, String categoria,
            LocalDateTime fechaAsignacion, LocalDateTime fechaEstado,
            NecesidadDeDonacionResponse necesidad, List<String> fotosRecepcion) {
        this(id, bienes, estadoActual, fecha, donanteId, entidadAsignadaId, subcategoria, categoria,
                fechaAsignacion, fechaEstado, necesidad, fotosRecepcion, null);
    }

    public String estado() {
        return estadoActual;
    }

    public String descripcion() {
        return subcategoria != null ? subcategoria : "";
    }
}
