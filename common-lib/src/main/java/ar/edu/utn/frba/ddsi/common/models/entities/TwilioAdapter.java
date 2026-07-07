package ar.edu.utn.frba.ddsi.common.models.entities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Component
class TwilioAdapter implements ProveedorSMS, ProveedorWhatsapp {

    private final String smsNumber;
    private final String whatsappNumber;

    public TwilioAdapter(
            @Value("${twilio.sid}") String sid,
            @Value("${twilio.token}") String token,
            @Value("${twilio.smsNumber}") String smsNumber,
            @Value("${twilio.whatsappNumber}") String whatsappNumber) {
        this.smsNumber = smsNumber;
        this.whatsappNumber = whatsappNumber;
        System.out.println("Twilio inicializado con SID: " + sid);
        Twilio.init(sid, token);
    }

    @Override
    public void enviarSMS(String destino, String mensaje) {

        String origen = smsNumber;

        System.out.println("listo Twilio para enviar");
        Message.creator(
                new PhoneNumber(destino),
                new PhoneNumber(origen),
                mensaje).create();
    }

    @Override
    public void enviarWhatsapp(String destino, String mensaje) {

        String origen = whatsappNumber;

        System.out.println("listo Twilio para enviar");

        Message.creator(
                new PhoneNumber("whatsapp:" + destino),
                new PhoneNumber("whatsapp:" + origen),
                mensaje).create();
    }
}
