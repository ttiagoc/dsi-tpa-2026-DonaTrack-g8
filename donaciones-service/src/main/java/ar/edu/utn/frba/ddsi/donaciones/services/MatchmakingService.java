package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaMatchmakingResponse;

public interface MatchmakingService {

    void ejecutarProcesoNocturno();

    List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes();

    void procesarMatchmakingGlobal();

    void aceptarPropuesta(Long propuestaId, Long entidadId);

    void rechazarPropuesta(Long propuestaId);
}
