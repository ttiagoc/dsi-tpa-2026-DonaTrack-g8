package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;

public interface ResultadoMatchmakingRepository {

    ResultadoMatchmaking save(ResultadoMatchmaking propuesta);

    Optional<ResultadoMatchmaking> findById(Long id);

    List<ResultadoMatchmaking> findAll();

    List<ResultadoMatchmaking> buscarPendientes();

    void limpiar();
}
