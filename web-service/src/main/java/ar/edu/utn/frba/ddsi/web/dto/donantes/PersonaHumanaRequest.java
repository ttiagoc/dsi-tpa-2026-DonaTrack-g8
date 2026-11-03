package ar.edu.utn.frba.ddsi.web.dto.donantes;

import java.time.LocalDate;
import java.util.List;

public record PersonaHumanaRequest(
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String dni,
        String genero,
        String direccion,
        List<MedioContactoDto> contactos,
        MedioContactoDto contactoPredeterminado
) {
}

