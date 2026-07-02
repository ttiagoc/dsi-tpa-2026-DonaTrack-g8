package ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking;

import java.util.List;

public record ObtenerPropuestasPendientesResponse(
    List<PropuestaMatchmakingInfo> propuestas
) {}
