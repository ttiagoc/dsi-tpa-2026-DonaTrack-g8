package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.MedioContacto;
import ar.edu.utn.frba.ddsi.common.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.donaciones.repositories.DonanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ImportadorDonantesService {

  private final DonanteRepository donanteRepository;

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
        // Ignorar líneas vacías
        if (linea.trim().isEmpty()) {
          continue;
        }

        // Ignorar la cabecera del CSV
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
    // Soportar separador por comas (o punto y coma si fuera necesario)
    String[] campos = linea.split(",");
    if (campos.length < 5) {
      // Línea inválida o incompleta
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
    Optional<PersonaHumana> existenteOpt = donanteRepository.buscarHumanaPorEmail(emailVal);

    if (existenteOpt.isPresent()) {
      // Actualizar información existente
      PersonaHumana humana = existenteOpt.get();
      actualizarNombreHumana(humana, nombreCompleto);
      humana.setDni(dni);
      actualizarContactos(humana.getContactos(), emailVal, telefonoVal);
      donanteRepository.guardarHumana(humana);
    } else {
      // Crear nuevo registro
      Donante donante = new Donante();
      PersonaHumana humana = new PersonaHumana();
      humana.setDonante(donante);
      actualizarNombreHumana(humana, nombreCompleto);
      humana.setDni(dni);

      List<MedioContacto> contactos = new ArrayList<>();
      Email email = new Email();
      email.setValor(emailVal);
      contactos.add(email);

      if (!telefonoVal.isEmpty()) {
        Telefono tel = new Telefono();
        tel.setValor(telefonoVal);
        contactos.add(tel);
      }

      humana.setContactos(contactos);
      humana.setContactoPredeterminado(email);

      donanteRepository.guardarHumana(humana);

      // Notificación de bienvenida simulada
      humana.getContactoPredeterminado().notificar(
          "Bienvenido/a a DonaTrack, " + nombreCompleto + "! Tu usuario ha sido creado con éxito."
      );
    }
  }

  private void procesarJuridica(String documento, String razonSocial, String emailVal, String telefonoVal) {
    Optional<PersonaJuridica> existenteOpt = donanteRepository.buscarJuridicaPorEmail(emailVal);

    if (existenteOpt.isPresent()) {
      // Actualizar información existente
      PersonaJuridica juridica = existenteOpt.get();
      juridica.setRazonSocial(razonSocial);
      juridica.setCuit(documento);
      actualizarContactos(juridica.getContactos(), emailVal, telefonoVal);
      donanteRepository.guardarJuridica(juridica);
    } else {
      // Crear nuevo registro
      Donante donante = new Donante();
      PersonaJuridica juridica = new PersonaJuridica();
      juridica.setDonante(donante);
      juridica.setRazonSocial(razonSocial);
      juridica.setCuit(documento);
      juridica.setTipo(TipoOrganizacion.EMPRESA); // Valor por defecto
      juridica.setRepresentantes(new ArrayList<>());

      List<MedioContacto> contactos = new ArrayList<>();
      Email email = new Email();
      email.setValor(emailVal);
      contactos.add(email);

      if (!telefonoVal.isEmpty()) {
        Telefono tel = new Telefono();
        tel.setValor(telefonoVal);
        contactos.add(tel);
      }

      juridica.setContactos(contactos);
      juridica.setContactoPredeterminado(email);

      donanteRepository.guardarJuridica(juridica);

      // Notificación de bienvenida simulada
      juridica.getContactoPredeterminado().notificar(
          "Bienvenido/a a DonaTrack, " + razonSocial + "! El usuario de su organización ha sido creado con éxito."
      );
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
    if (contactos == null) return;

    // Buscar y actualizar Email
    Optional<Email> emailOpt = contactos.stream()
        .filter(c -> c instanceof Email)
        .map(c -> (Email) c)
        .findFirst();
    emailOpt.ifPresent(email -> email.setValor(emailVal));

    // Buscar y actualizar o agregar Telefono
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
      Telefono tel = new Telefono();
      tel.setValor(telefonoVal);
      contactos.add(tel);
    }
  }
}
