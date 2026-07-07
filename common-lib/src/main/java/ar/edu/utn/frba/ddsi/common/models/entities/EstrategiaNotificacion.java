package ar.edu.utn.frba.ddsi.common.models.entities;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;

public interface EstrategiaNotificacion {
  TipoContacto getTipoContacto();

  void notificar(String valor, String mensaje);
}
