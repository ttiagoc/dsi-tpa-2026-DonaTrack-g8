package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.impl;

import java.util.List;
import java.util.Optional;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

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
    return createQuery(
        "from " + clase.getSimpleName(),
        clase
    ).getResultList();
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
    return entityManager()
        .getEntityManagerFactory()
        .getPersistenceUnitUtil()
        .getIdentifier(entidad);
  }
}