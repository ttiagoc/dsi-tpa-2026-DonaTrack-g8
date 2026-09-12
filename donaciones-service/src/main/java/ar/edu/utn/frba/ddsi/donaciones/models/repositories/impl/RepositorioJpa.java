package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.List;
import java.util.Optional;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Operaciones comunes de los repositorios sobre JPA (jpa-extras).
 * Cada escritura corre en su propia transaccion; el EntityManager es por hilo
 * y se libera al terminar cada request (ver PersistenceContextController).
 */
public abstract class RepositorioJpa<T> implements WithSimplePersistenceUnit {

    private final Class<T> clase;

    protected RepositorioJpa(Class<T> clase) {
        this.clase = clase;
    }

    public T save(T entidad) {
        return withTransaction(() -> {
            if (idDe(entidad) == null) {
                persist(entidad);
                return entidad;
            }
            // si ya esta administrada por el EntityManager, sus cambios se guardan al confirmar la transaccion
            if (entityManager().contains(entidad)) {
                return entidad;
            }
            return merge(entidad);
        });
    }

    public Optional<T> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(find(clase, id));
    }

    public List<T> findAll() {
        return createQuery("from " + clase.getSimpleName(), clase).getResultList();
    }

    public boolean deleteById(Long id) {
        return withTransaction(() -> {
            Optional<T> entidad = findById(id);
            entidad.ifPresent(this::remove);
            return entidad.isPresent();
        });
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    private Object idDe(T entidad) {
        return entityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entidad);
    }
}
