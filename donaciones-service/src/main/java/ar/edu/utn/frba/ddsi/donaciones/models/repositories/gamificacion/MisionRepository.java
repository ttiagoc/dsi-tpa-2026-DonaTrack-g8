package ar.edu.utn.frba.ddsi.donaciones.models.repositories.gamificacion;

import java.util.List;
import java.util.Optional;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.Mision;

public interface MisionRepository {
  List<Mision> findAll();
  Optional<Mision> findByCodigo(String codigo);
  Mision save(Mision mision);
}
