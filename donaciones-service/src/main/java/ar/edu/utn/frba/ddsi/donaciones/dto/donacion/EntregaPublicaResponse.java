package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.time.LocalDateTime;
import java.util.List;

// una donacion entregada, tal como se muestra en el mapa publico (req. 2): sin datos del donante
public record EntregaPublicaResponse(
        Long donacionId,
        String subcategoria,
        String categoria,
        LocalDateTime fechaEntrega,
        List<BienEntregadoResponse> bienes,
        List<String> fotos) {

    public record BienEntregadoResponse(String descripcion, Long cantidad) {
    }
}
