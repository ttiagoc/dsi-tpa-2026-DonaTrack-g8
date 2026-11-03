package ar.edu.utn.frba.ddsi.donaciones.dto.gamificacion;

import java.util.List;

public record IncentivosResponse (
    Long donanteId,
    String categoria,
    String tituloCategoria,
    int totalDonaciones,
    List<MisionProgresoResponse> misiones,
    List<InsigniaResponse> insigniasObtenidas
) {}
