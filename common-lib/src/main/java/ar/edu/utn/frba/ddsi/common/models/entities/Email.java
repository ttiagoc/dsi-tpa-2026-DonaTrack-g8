package ar.edu.utn.frba.ddsi.common.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Email implements MedioContacto {
  private String valor;

  @Override
  public void notificar(String mensaje) {
    System.out.println("[SMTP Server] Enviando correo electrÃ³nico a <" + this.valor + ">: " + mensaje);
  }
}
