package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.common.utils.GeneradorIdSecuencial;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Repository
public class InMemoryDonante implements DonanteRepository {
  private List<Donante> donantes = new ArrayList<>();
  private GeneradorIdSecuencial generadorId = new GeneradorIdSecuencial();

  public Donante save(Donante donante) {
    if (donante.getId() == null) {
      donante.setId(generadorId.siguiente());
      donantes.add(donante);
    } else {
      findById(donante.getId()).ifPresent(donantes::remove);
      donantes.add(donante);
    }
    return donante;
  }

  public Optional<Donante> findById(Long id) {
    if (id == null)
      return Optional.empty();
    return donantes.stream()
        .filter(d -> id.equals(d.getId()))
        .findFirst();
  }

  public List<Donante> findAll() {
    return new ArrayList<>(donantes);
  }

  public boolean deleteById(Long id) {
    Optional<Donante> donante = findById(id);
    if (donante.isPresent()) {
      donantes.remove(donante.get());
      return true;
    }
    return false;
  }

  public Optional<Donante> buscarPorEmail(String email) {
    if (email == null)
      return Optional.empty();
    return donantes.stream()
        .filter(d -> tieneEmail(d.getContactos(), email))
        .findFirst();
  }

  private boolean tieneEmail(List<MedioContacto> contactos, String emailBuscado) {
    if (contactos == null)
      return false;
    return contactos.stream()
        .filter(c -> c.getTipoContacto() == TipoContacto.EMAIL)
        .map(c -> c.getValor())
        .anyMatch(val -> val != null && val.equalsIgnoreCase(emailBuscado.trim()));
  }

  public boolean existsById(Long id) {
    return findById(id).isPresent();
  }
}
