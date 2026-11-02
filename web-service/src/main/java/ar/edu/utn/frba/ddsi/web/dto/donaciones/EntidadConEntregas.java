package ar.edu.utn.frba.ddsi.web.dto.donaciones;

import java.time.LocalDateTime;
import java.util.List;

/** Lo que devuelve GET /donaciones/entregadas (publico): cada entidad con las donaciones que ya recibio. */
public record EntidadConEntregas(
        Long entidadId,
        String razonSocial,
        String direccion,
        List<Entrega> entregas) {

    public record Entrega(Long donacionId, String subcategoria, String categoria, LocalDateTime fechaEntrega,
            List<DonacionResponse.Bien> bienes, List<String> fotos) {
    }
}
