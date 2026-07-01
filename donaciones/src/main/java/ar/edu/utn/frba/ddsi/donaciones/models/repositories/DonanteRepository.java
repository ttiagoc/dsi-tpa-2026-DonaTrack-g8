package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;

@Repository
public class DonanteRepository {
  private List<Donante> donantes = new ArrayList<>();
  private Long proximoId = 1L;

  /**
   * Guarda o actualiza un donante (Humana o Jurídica) en memoria.
   */
  public Donante save(Donante donante) {
    if (donante.getId() == null) {
      donante.setId(proximoId++);
      donantes.add(donante);
    } else {
      findById(donante.getId()).ifPresent(donantes::remove);
      donantes.add(donante);
    }
    return donante;
  }

  /**
   * Busca un donante por su ID único (Esencial para GET /api/donantes/{id}).
   */
  public Optional<Donante> findById(Long id) {
    if (id == null)
      return Optional.empty();
    return donantes.stream()
        .filter(d -> id.equals(d.getId()))
        .findFirst();
  }

  /**
   * Devuelve todos los donantes registrados (Para GET /api/donantes).
   */
  public List<Donante> findAll() {
    return new ArrayList<>(donantes);
  }

  /**
   * Elimina un donante por su ID (Para DELETE /api/donantes/{id}).
   */
  public boolean deleteById(Long id) {
    Optional<Donante> donante = findById(id);
    if (donante.isPresent()) {
      donantes.remove(donante.get());
      return true;
    }
    return false;
  }

  /**
   * Mantenemos la búsqueda por email útil para validaciones o el importador.
   */
  public Optional<Donante> buscarPorEmail(String email) {
    if (email == null)
      return Optional.empty();
    return donantes.stream()
        .filter(d -> tieneEmail(d.getContactos(), email))
        .findFirst();
  }

  public void limpiar() {
    donantes.clear();
    proximoId = 1L;
  }

  private boolean tieneEmail(List<MedioContacto> contactos, String emailBuscado) {
    if (contactos == null)
      return false;
    return contactos.stream()
        .filter(c -> c instanceof Email)
        .map(c -> ((Email) c).getValor())
        .anyMatch(val -> val != null && val.equalsIgnoreCase(emailBuscado.trim()));
  }

  public boolean existsById(Long id) {
    return findById(id).isPresent();
  }
}
