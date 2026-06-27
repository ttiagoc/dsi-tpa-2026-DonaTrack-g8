package ar.edu.utn.frba.ddsi.common.models.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsApp implements MedioContacto {
  private String valor;

  @Override
  public void notificar(String mensaje) {
    System.out.println("[WhatsApp Business API] Enviando mensaje a " + this.valor + ": " + mensaje);
  }
}