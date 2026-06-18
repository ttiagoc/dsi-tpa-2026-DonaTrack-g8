package ar.edu.utn.frba.ddsi.logistica.repositories;

import ar.edu.utn.frba.ddsi.logistica.models.Ruta;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RutaRepository {
    private final List<Ruta> rutas = new ArrayList<>();
    private Long sequence = 1L;

    public Ruta guardar(Ruta ruta) {
        if (ruta.getId() == null) {
            ruta.setId(sequence++);
            rutas.add(ruta);
        } else {
            buscarPorId(ruta.getId()).ifPresent(rutas::remove);
            rutas.add(ruta);
        }
        return ruta;
    }

    public Optional<Ruta> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return rutas.stream().filter(r -> id.equals(r.getId())).findFirst();
    }

    public List<Ruta> obtenerTodas() {
        return new ArrayList<>(rutas);
    }
}
