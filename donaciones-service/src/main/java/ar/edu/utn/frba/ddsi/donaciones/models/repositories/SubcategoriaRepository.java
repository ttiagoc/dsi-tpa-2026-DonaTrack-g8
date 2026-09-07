package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;

public interface SubcategoriaRepository {

    Subcategoria save(Subcategoria subcategoria);

    Optional<Subcategoria> findById(Long id);

    List<Subcategoria> findAll();

    Optional<Subcategoria> findByNombre(String nombre);

    boolean deleteById(Long id);
}
