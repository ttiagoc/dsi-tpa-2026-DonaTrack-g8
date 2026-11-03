package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.gamificacion.IncentivosResponse;

public interface GamificacionService {
  IncentivosResponse obtenerIncentivos(Long donanteId);
  void evaluarProgreso(Long donanteId);
}
