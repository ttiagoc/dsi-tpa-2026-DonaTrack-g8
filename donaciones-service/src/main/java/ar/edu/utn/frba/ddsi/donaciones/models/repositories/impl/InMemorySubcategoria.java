package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.utils.GeneradorIdSecuencial;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;

@Repository
public class InMemorySubcategoria implements SubcategoriaRepository {
    private final List<Subcategoria> subcategorias = new ArrayList<>();
    private final GeneradorIdSecuencial generadorId = new GeneradorIdSecuencial();

    @Override
    public Subcategoria save(Subcategoria subcategoria) {
        if (subcategoria.getId() == null) {
            subcategoria.setId(generadorId.siguiente());
            subcategorias.add(subcategoria);
        } else {
            findById(subcategoria.getId()).ifPresent(subcategorias::remove);
            subcategorias.add(subcategoria);
        }
        return subcategoria;
    }

    @Override
    public Optional<Subcategoria> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return subcategorias.stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst();
    }

    @Override
    public List<Subcategoria> findAll() {
        return new ArrayList<>(subcategorias);
    }

    @Override
    public Optional<Subcategoria> findByNombre(String nombre) {
        if (nombre == null)
            return Optional.empty();
        return subcategorias.stream()
                .filter(s -> nombre.equalsIgnoreCase(s.getNombre()))
                .findFirst();
    }

    @Override
    public boolean deleteById(Long id) {
        Optional<Subcategoria> subcategoria = findById(id);
        if (subcategoria.isPresent()) {
            subcategorias.remove(subcategoria.get());
            return true;
        }
        return false;
    }
}
