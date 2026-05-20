package ar.edu.utn.frba.ddsi.donaciones.repositories;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.PersonaJuridica;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DonanteRepository {
  private final List<PersonaHumana> personasHumanas = new ArrayList<>();
  private final List<PersonaJuridica> personasJuridicas = new ArrayList<>();

  public void guardarHumana(PersonaHumana persona) {
    // Si ya existe en la lista (por documento o por email anterior), la reemplazamos
    buscarHumanaPorEmail(obtenerEmailDePersona(persona.getContactos()))
        .ifPresent(personasHumanas::remove);
    personasHumanas.add(persona);
  }

  public void guardarJuridica(PersonaJuridica persona) {
    buscarJuridicaPorEmail(obtenerEmailDePersona(persona.getContactos()))
        .ifPresent(personasJuridicas::remove);
    personasJuridicas.add(persona);
  }

  public Optional<PersonaHumana> buscarHumanaPorEmail(String email) {
    if (email == null) return Optional.empty();
    return personasHumanas.stream()
        .filter(p -> tieneEmail(p.getContactos(), email))
        .findFirst();
  }

  public Optional<PersonaJuridica> buscarJuridicaPorEmail(String email) {
    if (email == null) return Optional.empty();
    return personasJuridicas.stream()
        .filter(p -> tieneEmail(p.getContactos(), email))
        .findFirst();
  }

  public List<PersonaHumana> obtenerTodasLasHumanas() {
    return new ArrayList<>(personasHumanas);
  }

  public List<PersonaJuridica> obtenerTodasLasJuridicas() {
    return new ArrayList<>(personasJuridicas);
  }

  public void limpiar() {
    personasHumanas.clear();
    personasJuridicas.clear();
  }

  private boolean tieneEmail(List<MedioContacto> contactos, String emailBuscado) {
    if (contactos == null) return false;
    return contactos.stream()
        .filter(c -> c instanceof Email)
        .map(c -> ((Email) c).getValor())
        .anyMatch(val -> val != null && val.equalsIgnoreCase(emailBuscado.trim()));
  }

  private String obtenerEmailDePersona(List<MedioContacto> contactos) {
    if (contactos == null) return null;
    return contactos.stream()
        .filter(c -> c instanceof Email)
        .map(c -> ((Email) c).getValor())
        .findFirst()
        .orElse(null);
  }
}
