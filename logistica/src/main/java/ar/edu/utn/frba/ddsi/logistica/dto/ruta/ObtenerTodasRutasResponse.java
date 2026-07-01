package ar.edu.utn.frba.ddsi.logistica.dto.ruta;

import java.util.List;

public record ObtenerTodasRutasResponse(
    List<ObtenerRutaResponse> rutas
) {}
