package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidad;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class NecesidadRepository {
  private final List<Necesidad> necesidades = new ArrayList<>();
  private Long sequence = 1L;

  public Necesidad guardar(Necesidad necesidad) {
    if (necesidad.getId() == null) {
      necesidad.setId(sequence++);
      necesidades.add(necesidad);
    } else {
      buscarPorId(necesidad.getId()).ifPresent(necesidades::remove);
      necesidades.add(necesidad);
    }
    return necesidad;
  }

  public Optional<Necesidad> buscarPorId(Long id) {
    if (id == null) return Optional.empty();
    return necesidades.stream().filter(n -> id.equals(n.getId())).findFirst();
  }

  public List<Necesidad> obtenerTodas() {
    return new ArrayList<>(necesidades);
  }
  
  public void limpiar() {
    necesidades.clear();
    sequence = 1L;
  }
}
