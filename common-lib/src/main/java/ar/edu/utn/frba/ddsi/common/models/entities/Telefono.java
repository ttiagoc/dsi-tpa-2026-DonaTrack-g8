package ar.edu.utn.frba.ddsi.common.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Telefono implements MedioContacto {
  private String valor;

  @Override
  public void notificar(String mensaje) {
    System.out.println("[SMS Gateway] Enviando SMS a " + this.valor + ": " + mensaje);
  }
}