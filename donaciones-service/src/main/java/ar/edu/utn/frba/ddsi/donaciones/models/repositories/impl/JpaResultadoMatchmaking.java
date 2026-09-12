package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;

@Repository
public class JpaResultadoMatchmaking extends RepositorioJpa<ResultadoMatchmaking>
        implements ResultadoMatchmakingRepository {

    public JpaResultadoMatchmaking() {
        super(ResultadoMatchmaking.class);
    }

    @Override
    public List<ResultadoMatchmaking> buscarPendientes() {
        return createQuery("from ResultadoMatchmaking where estado = :estado", ResultadoMatchmaking.class)
                .setParameter("estado", EstadoPropuesta.PENDIENTE)
                .getResultList();
    }
}
