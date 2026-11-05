package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

@Component
@Primary
public class SmtpEmailAdapter implements ProveedorEmail {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public SmtpEmailAdapter(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviarEmail(String destino, String mensaje) {
        if (remitente == null || remitente.isBlank() || remitente.contains("ejemplo.com") || remitente.equals("mock-user")) {
            log.warn("SMTP no configurado con credenciales válidas (remitente: '{}'). Omitiendo despacho real a {}", remitente, destino);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(remitente);
            helper.setTo(destino);
            helper.setSubject("DonaTrack - Notificación");

            boolean esHtml = mensaje != null && mensaje.contains("<") && mensaje.contains(">");
            helper.setText(mensaje != null ? mensaje : "", esHtml);

            log.info("Despachando correo vía SMTP desde {} hacia {}...", remitente, destino);
            mailSender.send(mimeMessage);
            log.info("Correo enviado exitosamente a {}", destino);
        } catch (Exception e) {
            log.error("Error al enviar correo vía SMTP a {}: {}", destino, e.getMessage(), e);
            throw new RuntimeException("Error en despacho de correo vía SMTP: " + e.getMessage(), e);
        }
    }
}

