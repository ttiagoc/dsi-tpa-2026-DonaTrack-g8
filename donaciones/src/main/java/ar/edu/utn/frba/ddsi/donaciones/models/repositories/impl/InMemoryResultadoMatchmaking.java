package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryResultadoMatchmaking implements ResultadoMatchmakingRepository {
    private List<ResultadoMatchmaking> propuestas = new ArrayList<>();

    private Long proximoId = 1L;

    public ResultadoMatchmaking save(ResultadoMatchmaking propuesta) {
        if (propuesta.getId() == null) {
            propuesta.setId(proximoId++);
            propuestas.add(propuesta);
        } else {
            findById(propuesta.getId()).ifPresent(propuestas::remove);
            propuestas.add(propuesta);
        }
        return propuesta;
    }

    public Optional<ResultadoMatchmaking> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return propuestas.stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst();
    }

    public List<ResultadoMatchmaking> findAll() {
        return new ArrayList<>(propuestas);
    }

    public List<ResultadoMatchmaking> buscarPendientes() {
        return propuestas.stream()
                .filter(p -> p.getEstado() == EstadoPropuesta.PENDIENTE)
                .toList();
    }

    public void limpiar() {
        propuestas.clear();
        proximoId = 1L;
    }
}