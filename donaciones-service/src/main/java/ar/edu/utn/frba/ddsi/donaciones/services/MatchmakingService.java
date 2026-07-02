package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.ObtenerPropuestasPendientesResponse;

public interface MatchmakingService {

    void ejecutarProcesoNocturno();

    ObtenerPropuestasPendientesResponse obtenerPropuestasPendientes();

    void procesarMatchmakingGlobal();

    void aceptarPropuesta(Long propuestaId, Long entidadId);

    void rechazarPropuesta(Long propuestaId);
}
