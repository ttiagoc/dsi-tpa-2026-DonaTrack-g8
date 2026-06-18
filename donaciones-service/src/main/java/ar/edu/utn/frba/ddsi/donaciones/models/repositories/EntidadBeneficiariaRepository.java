package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EntidadBeneficiariaRepository {
  private final List<EntidadBeneficiaria> entidades = new ArrayList<>();
  private Long sequence = 1L;

  public EntidadBeneficiaria guardar(EntidadBeneficiaria entidad) {
    if (entidad.getId() == null) {
      entidad.setId(sequence++);
      entidades.add(entidad);
    } else {
      buscarPorId(entidad.getId()).ifPresent(entidades::remove);
      entidades.add(entidad);
    }
    return entidad;
  }

  public Optional<EntidadBeneficiaria> buscarPorId(Long id) {
    if (id == null) return Optional.empty();
    return entidades.stream().filter(e -> id.equals(e.getId())).findFirst();
  }

  public List<EntidadBeneficiaria> obtenerTodas() {
    return new ArrayList<>(entidades);
  }
  
  public void limpiar() {
    entidades.clear();
    sequence = 1L;
  }
}
