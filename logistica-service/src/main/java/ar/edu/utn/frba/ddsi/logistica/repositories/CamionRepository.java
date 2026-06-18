package ar.edu.utn.frba.ddsi.logistica.repositories;

import ar.edu.utn.frba.ddsi.logistica.models.Camion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CamionRepository {
    private final List<Camion> camiones = new ArrayList<>();
    private Long sequence = 1L;

    public Camion guardar(Camion camion) {
        if (camion.getId() == null) {
            camion.setId(sequence++);
            camiones.add(camion);
        } else {
            buscarPorId(camion.getId()).ifPresent(camiones::remove);
            camiones.add(camion);
        }
        return camion;
    }

    public Optional<Camion> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return camiones.stream().filter(c -> id.equals(c.getId())).findFirst();
    }

    public List<Camion> obtenerTodos() {
        return new ArrayList<>(camiones);
    }
}
