package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportarCsv {

    public List<Donante> importar(String pathArchivo) {
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
        MedioContacto email = new MedioContacto(emailVal, new Email());
        contactos.add(email);

        if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, new Telefono());
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
        MedioContacto email = new MedioContacto(emailVal, new Email());
        contactos.add(email);

        if (!telefonoVal.isEmpty()) {
            MedioContacto tel = new MedioContacto(telefonoVal, new Telefono());
            contactos.add(tel);
        }

        juridica.setContactos(contactos);
        juridica.setContactoPredeterminado(email);

        return juridica;
    }
}
