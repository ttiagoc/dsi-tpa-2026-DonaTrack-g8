package ar.edu.utn.frba.ddsi.web.dto.donantes;

import java.time.LocalDate;
import java.util.List;

import ar.edu.utn.frba.ddsi.web.dto.registro.RepresentanteRequest;

/**
 * Lo que devuelve GET /donantes/{id}/detalle: todos los datos del donante, para verlo y editarlo.
 * tipoPersona es HUMANA o JURIDICA; los campos del otro tipo llegan en null.
 */
public record DonanteDetalle(
        Long id,
        String tipoPersona,
        String nombre,
        String apellido,
        String dni,
        LocalDate fechaNacimiento,
        String genero,
        String direccion,
        String razonSocial,
        String rubro,
        String tipoOrganizacion,
        String cuit,
        List<RepresentanteRequest> representantes,
        List<MedioContactoDto> contactos,
        MedioContactoDto contactoPredeterminado,
        int cantidadDonaciones,
        LocalDate ultimaDonacion) {

    public boolean esJuridica() {
        return "JURIDICA".equalsIgnoreCase(tipoPersona);
    }

    public String nombreCompleto() {
        if (esJuridica()) {
            return razonSocial;
        }
        return apellido == null || apellido.isBlank() ? nombre : nombre + " " + apellido;
    }

    public String documento() {
        return esJuridica() ? cuit : dni;
    }

    public String tipoEtiqueta() {
        return esJuridica() ? "Persona jurídica" : "Persona humana";
    }

    public String tipoOrganizacionEtiqueta() {
        if (tipoOrganizacion == null) {
            return null;
        }
        return switch (tipoOrganizacion) {
            case "EMPRESA" -> "Empresa";
            case "ONG" -> "ONG";
            case "INSTITUCION" -> "Institución";
            case "GUBERNAMENTAL" -> "Gubernamental";
            default -> tipoOrganizacion;
        };
    }

    /** El primer email: el que se edita en el formulario. */
    public MedioContactoDto email() {
        return contactosOVacio().stream().filter(c -> "EMAIL".equalsIgnoreCase(c.tipo())).findFirst().orElse(null);
    }

    /** El primer teléfono (SMS o WhatsApp): el que se edita en el formulario. */
    public MedioContactoDto telefono() {
        return contactosOVacio().stream().filter(c -> !"EMAIL".equalsIgnoreCase(c.tipo())).findFirst().orElse(null);
    }

    /** Los contactos que no se ven en el formulario (un segundo email, otro teléfono): se conservan al editar. */
    public List<MedioContactoDto> contactosExtra() {
        MedioContactoDto email = email();
        MedioContactoDto telefono = telefono();
        return contactosOVacio().stream().filter(c -> c != email && c != telefono).toList();
    }

    public boolean prefiereTelefono() {
        return contactoPredeterminado != null && !"EMAIL".equalsIgnoreCase(contactoPredeterminado.tipo());
    }

    public static String etiquetaContacto(String tipo) {
        if (tipo == null) {
            return "";
        }
        return switch (tipo.toUpperCase()) {
            case "EMAIL" -> "Email";
            case "WHATSAPP" -> "WhatsApp";
            case "SMS" -> "SMS";
            default -> tipo;
        };
    }

    public String contactoPredeterminadoEtiqueta() {
        if (contactoPredeterminado == null) {
            return null;
        }
        return etiquetaContacto(contactoPredeterminado.tipo()) + ": " + contactoPredeterminado.valor();
    }

    private List<MedioContactoDto> contactosOVacio() {
        return contactos == null ? List.of() : contactos;
    }
}
