package ar.edu.utn.frba.ddsi.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class WhatsApp implements MedioContacto {
  private String valor;

  @Override
  public void notificar(String mensaje) {
    // TODO: Implementar
  }
}