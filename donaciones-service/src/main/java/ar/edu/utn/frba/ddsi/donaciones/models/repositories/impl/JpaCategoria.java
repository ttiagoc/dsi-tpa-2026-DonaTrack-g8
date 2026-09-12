package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.CategoriaRepository;

@Repository
public class JpaCategoria extends RepositorioJpa<Categoria> implements CategoriaRepository {

    public JpaCategoria() {
        super(Categoria.class);
    }

    @Override
    public Optional<Categoria> findByNombre(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        return createQuery("from Categoria where lower(nombre) = lower(:nombre)", Categoria.class)
                .setParameter("nombre", nombre)
                .getResultStream()
                .findFirst();
    }
}
