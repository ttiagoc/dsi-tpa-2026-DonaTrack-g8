package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donacion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DonacionRepository {
  private final List<Donacion> donaciones = new ArrayList<>();
  private Long sequence = 1L;

  public Donacion guardar(Donacion donacion) {
    if (donacion.getId() == null) {
      donacion.setId(sequence++);
      donaciones.add(donacion);
    } else {
      buscarPorId(donacion.getId()).ifPresent(donaciones::remove);
      donaciones.add(donacion);
    }
    return donacion;
  }

  public Optional<Donacion> buscarPorId(Long id) {
    if (id == null) return Optional.empty();
    return donaciones.stream().filter(d -> id.equals(d.getId())).findFirst();
  }

  public List<Donacion> obtenerTodas() {
    return new ArrayList<>(donaciones);
  }
  
  public void limpiar() {
    donaciones.clear();
    sequence = 1L;
  }
}
