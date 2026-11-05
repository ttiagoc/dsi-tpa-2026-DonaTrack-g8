package ar.edu.utn.frba.ddsi.web.dto.donaciones;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        List<String> fotosRecepcion,
        String justificacion,
        Long donanteId) {

    public record Bien(String descripcion, Long cantidad) {
    }

    public record Necesidad(Long id, String subcategoria, String descripcion) {
    }

    public String bienesResumen() {
        if (bienes == null || bienes.isEmpty()) {
            return "-";
        }
        return bienes.stream()
                .map(b -> (b.cantidad() != null ? b.cantidad() + " x " : "") + b.descripcion())
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
    }

    public long totalUnidades() {
        if (bienes == null) {
            return 0;
        }
        return bienes.stream().mapToLong(b -> b.cantidad() != null ? b.cantidad() : 0).sum();
    }

    public String fechaFormateada() {
        LocalDateTime f = fechaEstado != null ? fechaEstado : fecha;
        if (f == null) {
            return "-";
        }
        return f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public boolean esEnDeposito() {
        return "EN_DEPOSITO".equalsIgnoreCase(estadoActual);
    }

    public boolean esVencida() {
        return "VENCIDA".equalsIgnoreCase(estadoActual);
    }
}
