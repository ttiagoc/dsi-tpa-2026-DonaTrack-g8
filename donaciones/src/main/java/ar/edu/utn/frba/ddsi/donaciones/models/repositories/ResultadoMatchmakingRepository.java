package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ResultadoMatchmakingRepository {
    private List<ResultadoMatchmaking> propuestas = new ArrayList<>();

    private Long proximoId = 1L;

    /**
     * Guarda una nueva propuesta o actualiza su estado (ej: cuando el admin
     * acepta/rechaza).
     */
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

    /**
     * Devuelve las propuestas que el proceso nocturno calculó y el administrador
     * todavía no confirmó ni rechazó.
     */
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