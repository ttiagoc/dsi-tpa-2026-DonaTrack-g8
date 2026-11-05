package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

import java.util.List;

// una entidad con las donaciones que ya recibio (mapa publico, req. 2); entregas: la mas reciente primero.
// latitud y longitud son nulas si la direccion todavia no se pudo geocodificar
public record EntidadConEntregasResponse(
        Long entidadId,
        String razonSocial,
        String direccion,
        Double latitud,
        Double longitud,
        List<EntregaPublicaResponse> entregas) {
}
