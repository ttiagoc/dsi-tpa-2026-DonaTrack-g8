package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

public record ActualizarPersonaHumanaResponse(
    Long id,
    String nombre,
    String apellido,
    String dni
) {}
