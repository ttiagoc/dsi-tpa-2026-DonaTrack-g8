package ar.edu.utn.frba.ddsi.donaciones.models.repositories.gamificacion;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.Mision;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.RepositorioJpa;

@Repository
public class JpaMision extends RepositorioJpa<Mision> implements MisionRepository {

  public JpaMision() {
    super(Mision.class);
  }

  @Override
  public Optional<Mision> findByCodigo(String codigo) {
    return createQuery("from Mision where codigo = :codigo", Mision.class)
        .setParameter("codigo", codigo)
        .getResultStream()
        .findFirst();
  }
}
