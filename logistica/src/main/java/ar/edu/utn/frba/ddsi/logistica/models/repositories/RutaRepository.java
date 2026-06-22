package ar.edu.utn.frba.ddsi.logistica.models.repositories;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RutaRepository {
    private List<Ruta> rutas = new ArrayList<>();

    private Long proximoId = 1L;

    public Ruta save(Ruta ruta) {
        if (ruta.getId() == null) {
            ruta.setId(proximoId++);
            rutas.add(ruta);
        } else {
            findById(ruta.getId()).ifPresent(rutas::remove);
            rutas.add(ruta);
        }
        return ruta;
    }

    public Optional<Ruta> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return rutas.stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst();
    }

    public List<Ruta> findAll() {
        return new ArrayList<>(rutas);
    }

    public List<Ruta> buscarRutasActivas() {
        return rutas.stream()
                .filter(r -> r.getEstado() == EstadoRuta.EN_TRASLADO)
                .toList();
    }

    public boolean deleteById(Long id) {
        Optional<Ruta> ruta = findById(id);
        if (ruta.isPresent()) {
            rutas.remove(ruta.get());
            return true;
        }
        return false;
    }

    public void limpiar() {
        rutas.clear();
        proximoId = 1L;
    }
}