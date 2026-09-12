package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;

@Repository
public class JpaSubcategoria extends RepositorioJpa<Subcategoria> implements SubcategoriaRepository {

    public JpaSubcategoria() {
        super(Subcategoria.class);
    }

    @Override
    public List<Subcategoria> findByCategoriaId(Long categoriaId) {
        return createQuery("from Subcategoria where categoria.id = :categoriaId", Subcategoria.class)
                .setParameter("categoriaId", categoriaId)
                .getResultList();
    }

    @Override
    public Optional<Subcategoria> findByNombre(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        return createQuery("from Subcategoria where lower(nombre) = lower(:nombre)", Subcategoria.class)
                .setParameter("nombre", nombre)
                .getResultStream()
                .findFirst();
    }
}
