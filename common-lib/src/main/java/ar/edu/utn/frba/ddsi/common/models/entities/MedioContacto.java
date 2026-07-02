package ar.edu.utn.frba.ddsi.common.models.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedioContacto {
  private String valor;
  private NotificacionStrategy estrategia;

  public void notificar(String mensaje) {
    if (estrategia != null) {
      estrategia.notificar(this.valor, mensaje);
    }
  }
}
