package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.utils.GeneradorIdSecuencial;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.CategoriaRepository;

@Repository
public class InMemoryCategoria implements CategoriaRepository {
    private final List<Categoria> categorias = new ArrayList<>();
    private final GeneradorIdSecuencial generadorId = new GeneradorIdSecuencial();

    @Override
    public Categoria save(Categoria categoria) {
        if (categoria.getId() == null) {
            categoria.setId(generadorId.siguiente());
            categorias.add(categoria);
        } else {
            findById(categoria.getId()).ifPresent(categorias::remove);
            categorias.add(categoria);
        }
        return categoria;
    }

    @Override
    public Optional<Categoria> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return categorias.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst();
    }

    @Override
    public List<Categoria> findAll() {
        return new ArrayList<>(categorias);
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        if (nombre == null)
            return Optional.empty();
        return categorias.stream()
                .filter(c -> nombre.equalsIgnoreCase(c.getNombre()))
                .findFirst();
    }

    @Override
    public boolean deleteById(Long id) {
        Optional<Categoria> categoria = findById(id);
        if (categoria.isPresent()) {
            categorias.remove(categoria.get());
            return true;
        }
        return false;
    }
}
