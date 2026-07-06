package ar.edu.utn.frba.ddsi.common.models.entities;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

public class Email implements CanalContacto {

  private static final String RESEND_API_KEY = System.getenv("RESEND_API_KEY");
  private static final String FROM_EMAIL = System.getenv("RESEND_FROM_EMAIL");

  @Override
  public void notificar(String valor, String mensaje) {
    if (RESEND_API_KEY != null && FROM_EMAIL != null) {
      try {
        Resend resend = new Resend(RESEND_API_KEY);
        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(FROM_EMAIL)
            .to(valor)
            .subject("Notificación de DonaTrack")
            .html("<p>" + mensaje + "</p>")
            .build();

        CreateEmailResponse data = resend.emails().send(params);
        System.out.println("[Resend API] Email enviado con ID: " + data.getId());
      } catch (ResendException e) {
        System.err.println("[Resend API] Error enviando email a " + valor + ": " + e.getMessage());
      }
    } else {
      System.out.println("[SMTP Server] Resend no configurado. Simulando envío a <" + valor + ">: " + mensaje);
    }
  }
}
