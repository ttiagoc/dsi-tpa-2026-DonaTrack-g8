package ar.edu.utn.frba.ddsi.common.models.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Email implements MedioContacto {
  private String valor;

  @Override
  public void notificar(String mensaje) {
    System.out.println("[SMTP Server] Enviando correo electrÃ³nico a <" + this.valor + ">: " + mensaje);
  }
}
