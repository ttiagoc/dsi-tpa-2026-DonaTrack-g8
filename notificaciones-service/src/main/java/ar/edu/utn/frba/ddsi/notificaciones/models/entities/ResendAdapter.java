package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

@Component
public class ResendAdapter implements ProveedorEmail {

    private final Resend resend;
    private final String origen;

    public ResendAdapter(
            @Value("${resend.apiKey}") String apiKey,
            @Value("${resend.from}") String origen) {
        this.resend = new Resend(apiKey);
        this.origen = origen;
    }

    @Override
    public void enviarEmail(String destino, String mensaje) {

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(this.origen)
                .to(destino)
                .subject("Notificación")
                .html(mensaje)
                .build();

        try {
            System.out.println("listo resend para enviar");
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println(data.getId());
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }
}
