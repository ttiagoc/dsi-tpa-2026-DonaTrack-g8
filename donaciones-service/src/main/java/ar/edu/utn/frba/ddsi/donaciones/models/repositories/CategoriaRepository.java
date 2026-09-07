package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;

public interface CategoriaRepository {

    Categoria save(Categoria categoria);

    Optional<Categoria> findById(Long id);

    List<Categoria> findAll();

    Optional<Categoria> findByNombre(String nombre);

    boolean deleteById(Long id);
}
