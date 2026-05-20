package ar.edu.utn.frba.ddsi.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Email implements MedioContacto {
  private String valor;

  @Override
  public void notificar(String mensaje) {
    System.out.println("[SMTP Server] Enviando correo electrónico a <" + this.valor + ">: " + mensaje);
  }
}
