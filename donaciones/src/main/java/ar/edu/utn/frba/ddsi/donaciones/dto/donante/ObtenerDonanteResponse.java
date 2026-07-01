package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

import java.util.List;

public record ObtenerDonanteResponse(
    Long id,
    String tipo,
    String nombre,
    List<MedioContactoInfo> contactos
) {}
