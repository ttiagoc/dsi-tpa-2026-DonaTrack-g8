package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

import java.time.LocalDate;

public record ActualizarPersonaHumanaRequest(
    String nombre,
    String apellido,
    LocalDate fechaNacimiento,
    String dni,
    String genero,
    String direccion
) {}
