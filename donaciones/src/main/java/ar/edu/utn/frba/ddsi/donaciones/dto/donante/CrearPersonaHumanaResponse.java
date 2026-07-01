package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

public record CrearPersonaHumanaResponse(
    Long id,
    String nombre,
    String apellido,
    String dni
) {}
