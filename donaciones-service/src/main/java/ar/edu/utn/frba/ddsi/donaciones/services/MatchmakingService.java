package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaMatchmakingResponse;

public interface MatchmakingService {

    List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes();

    void procesarMatchmaking();

    void aceptarPropuesta(Long propuestaId, Long entidadId);

    void rechazarPropuesta(Long propuestaId);
}
