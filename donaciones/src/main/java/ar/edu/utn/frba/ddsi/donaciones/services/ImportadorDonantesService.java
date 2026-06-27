package ar.edu.utn.frba.ddsi.donaciones.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Service
public class ImportadorDonantesService {

  private DonanteRepository donanteRepository;

  @Autowired
  public ImportadorDonantesService(DonanteRepository donanteRepository) {
    this.donanteRepository = donanteRepository;
  }

  public void importarDonantes(String pathArchivo) {
    if (pathArchivo == null || pathArchivo.isEmpty()) {
      throw new IllegalArgumentException("La ruta del archivo no puede estar vacía");
    }

    try (BufferedReader br = new BufferedReader(new FileReader(pathArchivo))) {
      String linea;
      boolean esCabecera = true;

      while ((linea = br.readLine()) != null) {
        if (linea.trim().isEmpty()) {
          continue;
        }

        if (esCabecera) {
          esCabecera = false;
          continue;
        }

        procesarLinea(linea);
      }
    } catch (IOException e) {
      throw new RuntimeException("Error al leer el archivo CSV: " + e.getMessage(), e);
    }
  }

  private void procesarLinea(String linea) {
    String[] campos = linea.split(",");
    if (campos.length < 5) {
      return;
    }

    String tipoPersona = campos[0].trim();
    String documento = campos[2].trim();
    String nombreORazonSocial = campos[3].trim();
    String emailVal = campos[4].trim();
    String telefonoVal = campos.length > 5 ? campos[5].trim() : "";

    if (tipoPersona.equalsIgnoreCase("HUMANA")) {
      procesarHumana(documento, nombreORazonSocial, emailVal, telefonoVal);
    } else if (tipoPersona.equalsIgnoreCase("JURIDICA")) {
      procesarJuridica(documento, nombreORazonSocial, emailVal, telefonoVal);
    }
  }

  private void procesarHumana(String dni, String nombreCompleto, String emailVal, String telefonoVal) {
    Optional<PersonaHumana> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
        .map(d -> (PersonaHumana) d);

    if (existenteOpt.isPresent()) {
      PersonaHumana humana = existenteOpt.get();
      actualizarNombreHumana(humana, nombreCompleto);
      humana.setDni(dni);
      actualizarContactos(humana.getContactos(), emailVal, telefonoVal);
      donanteRepository.save(humana);
    } else {
      PersonaHumana humana = new PersonaHumana();
      actualizarNombreHumana(humana, nombreCompleto);
      humana.setDni(dni);

      List<MedioContacto> contactos = new ArrayList<>();
      Email email = new Email(emailVal);
      contactos.add(email);

      if (!telefonoVal.isEmpty()) {
        Telefono tel = new Telefono(telefonoVal);
        contactos.add(tel);
      }

      humana.setContactos(contactos);
      humana.setContactoPredeterminado(email);

      donanteRepository.save(humana);

      humana.getContactoPredeterminado().notificar(
          "Bienvenido/a a DonaTrack, " + nombreCompleto + "! Tu usuario ha sido creado con Éxito.");
    }
  }

  private void procesarJuridica(String documento, String razonSocial, String emailVal, String telefonoVal) {
    Optional<PersonaJuridica> existenteOpt = donanteRepository.buscarPorEmail(emailVal)
        .map(d -> (PersonaJuridica) d);

    if (existenteOpt.isPresent()) {
      PersonaJuridica juridica = existenteOpt.get();
      juridica.setRazonSocial(razonSocial);
      juridica.setCuit(documento);
      actualizarContactos(juridica.getContactos(), emailVal, telefonoVal);
      donanteRepository.save(juridica);
    } else {
      PersonaJuridica juridica = new PersonaJuridica();
      juridica.setRazonSocial(razonSocial);
      juridica.setCuit(documento);
      juridica.setTipo(TipoOrganizacion.EMPRESA);
      juridica.setRepresentantes(new ArrayList<>());

      List<MedioContacto> contactos = new ArrayList<>();
      Email email = new Email(emailVal);
      contactos.add(email);

      if (!telefonoVal.isEmpty()) {
        Telefono tel = new Telefono(telefonoVal);
        contactos.add(tel);
      }

      juridica.setContactos(contactos);
      juridica.setContactoPredeterminado(email);

      donanteRepository.save(juridica);

      juridica.getContactoPredeterminado().notificar(
          "Bienvenido/a a DonaTrack, " + razonSocial + "! El usuario de su organización ha sido creado con Éxito.");
    }
  }

  private void actualizarNombreHumana(PersonaHumana humana, String nombreCompleto) {
    int primerEspacio = nombreCompleto.indexOf(' ');
    if (primerEspacio > 0) {
      humana.setNombre(nombreCompleto.substring(0, primerEspacio).trim());
      humana.setApellido(nombreCompleto.substring(primerEspacio).trim());
    } else {
      humana.setNombre(nombreCompleto.trim());
      humana.setApellido("");
    }
  }

  private void actualizarContactos(List<MedioContacto> contactos, String emailVal, String telefonoVal) {
    if (contactos == null)
      return;

    Optional<Email> emailOpt = contactos.stream()
        .filter(c -> c instanceof Email)
        .map(c -> (Email) c)
        .findFirst();
    emailOpt.ifPresent(email -> email.setValor(emailVal));

    Optional<Telefono> telOpt = contactos.stream()
        .filter(c -> c instanceof Telefono)
        .map(c -> (Telefono) c)
        .findFirst();
    if (telOpt.isPresent()) {
      if (!telefonoVal.isEmpty()) {
        telOpt.get().setValor(telefonoVal);
      } else {
        contactos.remove(telOpt.get());
      }
    } else if (!telefonoVal.isEmpty()) {
      Telefono tel = new Telefono(telefonoVal);
      contactos.add(tel);
    }
  }
}
