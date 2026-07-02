package ar.edu.utn.frba.ddsi.common.models.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedioContacto {
  private String valor;
  private CanalContacto canal;

  public void notificar(String mensaje) {
    if (canal != null) {
      canal.notificar(this.valor, mensaje);
    }
  }
}
