package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.internet.MimeMessage;

@DisplayName("SmtpEmailAdapter Tests")
class SmtpEmailAdapterTest {

    @Test
    @DisplayName("Cuando no hay credenciales configuradas (mock-user), omite el envío sin lanzar error")
    void omiteEnvioSiEsMockUser() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        SmtpEmailAdapter adapter = new SmtpEmailAdapter(sender, "mock-user");

        assertDoesNotThrow(() -> adapter.enviarEmail("test@ejemplo.com", "Mensaje de prueba"));
    }

    @Test
    @DisplayName("Cuando el remitente está configurado, invoca al mailSender para enviar")
    void enviaEmailCuandoEstaConfigurado() {
        AtomicBoolean fueEnviado = new AtomicBoolean(false);

        JavaMailSenderImpl sender = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                fueEnviado.set(true);
            }
        };

        SmtpEmailAdapter adapter = new SmtpEmailAdapter(sender, "donatrack.test@brevo.com");
        adapter.enviarEmail("destinatario@ejemplo.com", "<b>Hola</b> Donante");

        assertTrue(fueEnviado.get(), "Debe llamar a send(mimeMessage)");
    }
}

