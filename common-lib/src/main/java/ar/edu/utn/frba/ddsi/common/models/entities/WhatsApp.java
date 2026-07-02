package ar.edu.utn.frba.ddsi.common.models.entities;

public class WhatsApp implements CanalContacto {

  @Override
  public void notificar(String valor, String mensaje) {
    System.out.println("[WhatsApp Business API] Enviando mensaje a " + valor + ": " + mensaje);
  }
}