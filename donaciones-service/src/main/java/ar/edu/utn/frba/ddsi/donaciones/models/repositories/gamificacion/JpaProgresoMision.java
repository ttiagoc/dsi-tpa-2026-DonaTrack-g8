package ar.edu.utn.frba.ddsi.donaciones.models.repositories.gamificacion;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.ProgresoMision;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.RepositorioJpa;

@Repository
public class JpaProgresoMision extends RepositorioJpa<ProgresoMision> implements ProgresoMisionRepository{
  public JpaProgresoMision() {
    super(ProgresoMision.class);
  }
  @Override
  public List<ProgresoMision> findByDonanteId(Long donanteId) {
    return createQuery("FROM ProgresoMision WHERE donanteId = :donanteId", ProgresoMision.class)
        .setParameter("donanteId", donanteId)
        .getResultList();
  }
  @Override
  public Optional<ProgresoMision> findByDonanteIdAndMisionId(Long donanteId, Long misionId) {
    return createQuery("FROM ProgresoMision WHERE donanteId = :donanteId AND mision.id = :misionId", ProgresoMision.class)
        .setParameter("donanteId", donanteId)
        .setParameter("misionId", misionId)
        .getResultStream()
        .findFirst();
  }
}
