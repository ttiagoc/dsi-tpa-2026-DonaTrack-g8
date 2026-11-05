package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

import java.time.LocalDate;
import java.util.List;

/**
 * Todos los datos de un donante, para la pantalla de donantes del administrador (ver y editar).
 * tipoPersona es HUMANA o JURIDICA; los campos del otro tipo quedan en null.
 * Los medios de contacto usan los mismos tipos que el alta (EMAIL, SMS, WHATSAPP).
 */
public record DonanteDetalleResponse(
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
        List<MedioContactoRequest> contactos,
        MedioContactoRequest contactoPredeterminado,
        int cantidadDonaciones,
        LocalDate ultimaDonacion) {
}
