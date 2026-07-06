package ar.edu.utn.frba.ddsi.common.models.entities;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class Telefono implements CanalContacto {

  private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
  private static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
  private static final String TWILIO_PHONE = System.getenv("TWILIO_PHONE_NUMBER");

  @Override
  public void notificar(String valor, String mensaje) {
    if (ACCOUNT_SID != null && AUTH_TOKEN != null && TWILIO_PHONE != null) {
      Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
      Message twilioMessage = Message.creator(
          new com.twilio.type.PhoneNumber(valor),
          new com.twilio.type.PhoneNumber(TWILIO_PHONE),
          mensaje).create();
      System.out.println("[SMS Gateway] Enviado con SID: " + twilioMessage.getSid());
    } else {
      System.out.println("[SMS Gateway] Twilio no configurado. Simulando envío SMS a " + valor + ": " + mensaje);
    }
  }
}