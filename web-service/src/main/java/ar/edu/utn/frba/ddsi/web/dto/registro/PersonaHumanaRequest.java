package ar.edu.utn.frba.ddsi.web.dto.registro;

import java.time.LocalDate;
import java.util.List;

public record PersonaHumanaRequest(
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String dni,
        String genero,
        String direccion,
        List<MedioContactoRequest> contactos,
        MedioContactoRequest contactoPredeterminado) {
}
