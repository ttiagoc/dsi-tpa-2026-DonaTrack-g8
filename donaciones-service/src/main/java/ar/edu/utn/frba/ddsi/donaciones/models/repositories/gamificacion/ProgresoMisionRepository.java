package ar.edu.utn.frba.ddsi.donaciones.models.repositories.gamificacion;

import java.util.List;
import java.util.Optional;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion.ProgresoMision;

public interface ProgresoMisionRepository {
  List<ProgresoMision> findByDonanteId(Long donanteId);
  Optional<ProgresoMision> findByDonanteIdAndMisionId(Long donanteId, Long misionId);
  ProgresoMision save(ProgresoMision progresoMision);
}
