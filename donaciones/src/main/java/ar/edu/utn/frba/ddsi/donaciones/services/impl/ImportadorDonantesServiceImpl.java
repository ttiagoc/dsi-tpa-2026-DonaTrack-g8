package ar.edu.utn.frba.ddsi.donaciones.services.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.ImportarCsv;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.ImportadorDonantesService;

@Service
public class ImportadorDonantesServiceImpl implements ImportadorDonantesService {

  private final DonanteRepository donanteRepository;
  private final ImportarCsv importarCsv;

  public ImportadorDonantesServiceImpl(DonanteRepository donanteRepository, ImportarCsv importarCsv) {
    this.donanteRepository = donanteRepository;
    this.importarCsv = importarCsv;
  }

  public void importarDonantes(String pathArchivo) {
    List<Donante> donantes = importarCsv.importar(pathArchivo);
    for (Donante donante : donantes) {
      if (donante instanceof PersonaHumana) {
        procesarHumana((PersonaHumana) donante);
      } else if (donante instanceof PersonaJuridica) {
        procesarJuridica((PersonaJuridica) donante);
      }
    }
  }

  private void procesarHumana(PersonaHumana humanaNueva) {
    String emailVal = obtenerEmailVal(humanaNueva.getContactos());
    if (emailVal == null)
      return;

    Optional<PersonaHumana> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
        .map(d -> (PersonaHumana) d);

    if (existenteOpt.isPresent()) {
      PersonaHumana humana = existenteOpt.get();
      humana.setNombre(humanaNueva.getNombre());
      humana.setApellido(humanaNueva.getApellido());
      humana.setDni(humanaNueva.getDni());
      actualizarContactos(humana.getContactos(), emailVal, obtenerTelefonoVal(humanaNueva.getContactos()));
      donanteRepository.save(humana);
    } else {
      donanteRepository.save(humanaNueva);
      String nombreAMostrar = humanaNueva.getNombre()
          + (humanaNueva.getApellido().isEmpty() ? "" : " " + humanaNueva.getApellido());
      humanaNueva.getContactoPredeterminado().notificar(
          "Bienvenido/a a DonaTrack, " + nombreAMostrar.trim() + "! Tu usuario ha sido creado con Éxito.");
    }
  }

  private void procesarJuridica(PersonaJuridica juridicaNueva) {
    String emailVal = obtenerEmailVal(juridicaNueva.getContactos());
    if (emailVal == null)
      return;

    Optional<PersonaJuridica> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
        .map(d -> (PersonaJuridica) d);

    if (existenteOpt.isPresent()) {
      PersonaJuridica juridica = existenteOpt.get();
      juridica.setRazonSocial(juridicaNueva.getRazonSocial());
      juridica.setCuit(juridicaNueva.getCuit());
      actualizarContactos(juridica.getContactos(), emailVal, obtenerTelefonoVal(juridicaNueva.getContactos()));
      donanteRepository.save(juridica);
    } else {
      donanteRepository.save(juridicaNueva);
      juridicaNueva.getContactoPredeterminado().notificar(
          "Bienvenido/a a DonaTrack, " + juridicaNueva.getRazonSocial()
              + "! El usuario de su organización ha sido creado con Éxito.");
    }
  }

  private String obtenerEmailVal(List<MedioContacto> contactos) {
    if (contactos == null)
      return null;
    return contactos.stream()
        .filter(c -> c.getCanal() instanceof Email)
        .map(c -> c.getValor())
        .findFirst()
        .orElse(null);
  }

  private String obtenerTelefonoVal(List<MedioContacto> contactos) {
    if (contactos == null)
      return "";
    return contactos.stream()
        .filter(c -> c.getCanal() instanceof Telefono)
        .map(c -> c.getValor())
        .findFirst()
        .orElse("");
  }

  private void actualizarContactos(List<MedioContacto> contactos, String emailVal, String telefonoVal) {
    if (contactos == null)
      return;

    Optional<MedioContacto> emailOpt = contactos.stream()
        .filter(c -> c.getCanal() instanceof Email)
        .findFirst();

    if (emailOpt.isPresent()) {
      emailOpt.get().setValor(emailVal);
    } else {
      MedioContacto email = new MedioContacto(emailVal, new Email());
      contactos.add(email);
    }

    Optional<MedioContacto> telOpt = contactos.stream()
        .filter(c -> c.getCanal() instanceof Telefono)
        .findFirst();

    if (telOpt.isPresent()) {
      if (!telefonoVal.isEmpty()) {
        telOpt.get().setValor(telefonoVal);
      } else {
        contactos.remove(telOpt.get());
      }
    } else if (!telefonoVal.isEmpty()) {
      MedioContacto tel = new MedioContacto(telefonoVal, new Telefono());
      contactos.add(tel);
    }
  }
}
