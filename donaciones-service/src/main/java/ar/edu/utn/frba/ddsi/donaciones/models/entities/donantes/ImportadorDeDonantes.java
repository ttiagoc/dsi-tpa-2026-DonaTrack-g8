package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Component
public class ImportadorDeDonantes {

    private final DonanteRepository donanteRepository;
    private final RestTemplate restTemplate;
    private final RestDonacionesConfig config;

    public ImportadorDeDonantes(DonanteRepository donanteRepository, RestTemplate restTemplate,
            RestDonacionesConfig config) {
        this.donanteRepository = donanteRepository;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public void importarDonantes(String pathArchivo) {
        List<Donante> donantes = importarCsv(pathArchivo);
        for (Donante donante : donantes) {
            if (donante instanceof PersonaHumana) {
                procesarHumana((PersonaHumana) donante);
            } else if (donante instanceof PersonaJuridica) {
                procesarJuridica((PersonaJuridica) donante);
            }
        }
    }

    private List<Donante> importarCsv(String pathArchivo) {
        if (pathArchivo == null || pathArchivo.isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía");
        }

        List<Donante> donantes = new ArrayList<>();

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

                Donante donante = procesarLinea(linea);
                if (donante != null) {
                    donantes.add(donante);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo CSV: " + e.getMessage(), e);
        }

        return donantes;
    }

    private Donante procesarLinea(String linea) {
        String[] campos = linea.split(",");
        if (campos.length < 5) {
            return null;
        }

        String tipoPersona = campos[0].trim();
        String documento = campos[2].trim();
        String nombreORazonSocial = campos[3].trim();
        String emailVal = campos[4].trim();
        String telefonoVal = campos.length > 5 ? campos[5].trim() : "";

        if (tipoPersona.equalsIgnoreCase("HUMANA")) {
            return procesarHumana(documento, nombreORazonSocial, emailVal, telefonoVal);
        } else if (tipoPersona.equalsIgnoreCase("JURIDICA")) {
            return procesarJuridica(documento, nombreORazonSocial, emailVal, telefonoVal);
        }

        return null;
    }

    private PersonaHumana procesarHumana(String dni, String nombreCompleto, String emailVal, String telefonoVal) {
        PersonaHumana humana = new PersonaHumana();
        int primerEspacio = nombreCompleto.indexOf(' ');
        if (primerEspacio > 0) {
            humana.setNombre(nombreCompleto.substring(0, primerEspacio).trim());
            humana.setApellido(nombreCompleto.substring(primerEspacio).trim());
        } else {
            humana.setNombre(nombreCompleto.trim());
            humana.setApellido("");
        }
        humana.setDni(dni);

        List<MedioContacto> contactos = new ArrayList<>();
        MedioContacto email = new MedioContacto(emailVal, TipoContacto.EMAIL);
        contactos.add(email);

        if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, TipoContacto.SMS);
            contactos.add(tel);
        }

        humana.setContactos(contactos);
        humana.setContactoPredeterminado(email);

        return humana;
    }

    private PersonaJuridica procesarJuridica(String documento, String razonSocial, String emailVal,
            String telefonoVal) {
        PersonaJuridica juridica = new PersonaJuridica();
        juridica.setRazonSocial(razonSocial);
        juridica.setCuit(documento);
        juridica.setTipo("empresa");
        juridica.setRepresentantes(new ArrayList<>());

        List<MedioContacto> contactos = new ArrayList<>();
        MedioContacto email = new MedioContacto(emailVal, TipoContacto.EMAIL);
        contactos.add(email);

        if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, TipoContacto.SMS);
            contactos.add(tel);
        }

        juridica.setContactos(contactos);
        juridica.setContactoPredeterminado(email);

        return juridica;
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
            try {
                String url = config.getNotificacionesUrl() + "/notificaciones";
                NotificacionRequest request = new NotificacionRequest(
                        emailVal,
                        TipoContacto.EMAIL,
                        "¡Bienvenido a DonaTrack, " + nombreAMostrar + "! Tu cuenta ha sido registrada con éxito.");
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error notificando alta de donante humano: " + e.getMessage());
            }
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
            try {
                String url = config.getNotificacionesUrl() + "/notificaciones";
                NotificacionRequest request = new NotificacionRequest(
                        emailVal,
                        TipoContacto.EMAIL,
                        "¡Bienvenido a DonaTrack, " + juridicaNueva.getRazonSocial()
                                + "! Tu cuenta ha sido registrada con éxito.");
                restTemplate.postForObject(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error notificando alta de donante jurídico: " + e.getMessage());
            }
        }
    }

    private String obtenerEmailVal(List<MedioContacto> contactos) {
        if (contactos == null)
            return null;
        return contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.EMAIL)
                .map(c -> c.getValor())
                .findFirst()
                .orElse(null);
    }

    private String obtenerTelefonoVal(List<MedioContacto> contactos) {
        if (contactos == null)
            return "";
        return contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.SMS)
                .map(c -> c.getValor())
                .findFirst()
                .orElse("");
    }

    private void actualizarContactos(List<MedioContacto> contactos, String emailVal, String telefonoVal) {
        if (contactos == null)
            return;

        Optional<MedioContacto> emailOpt = contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.EMAIL)
                .findFirst();

        if (emailOpt.isPresent()) {
            emailOpt.get().setValor(emailVal);
        } else {
            MedioContacto email = new MedioContacto(emailVal, TipoContacto.EMAIL);
            contactos.add(email);
        }

        Optional<MedioContacto> telOpt = contactos.stream()
                .filter(c -> c.getTipoContacto() == TipoContacto.SMS)
                .findFirst();

        if (telOpt.isPresent()) {
            if (!telefonoVal.isEmpty()) {
                telOpt.get().setValor(telefonoVal);
            } else {
                contactos.remove(telOpt.get());
            }
        } else if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, TipoContacto.SMS);
            contactos.add(tel);
        }
    }
}
