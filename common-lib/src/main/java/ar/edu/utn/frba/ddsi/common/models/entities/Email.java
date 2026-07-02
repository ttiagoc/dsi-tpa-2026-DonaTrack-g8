package ar.edu.utn.frba.ddsi.common.models.entities;

public class Email implements NotificacionStrategy {

  @Override
  public void notificar(String valor, String mensaje) {
    System.out.println("[SMTP Server] Enviando correo electrónico a <" + valor + ">: " + mensaje);
  }
}
