package ar.edu.utn.frba.ddsi.donaciones.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "rest")
@Getter
@Setter
public class RestDonacionesConfig {
    private String notificacionesUrl;
    // la usan los mensajes que llevan un enlace al sitio (por ejemplo, la bienvenida con las credenciales)
    private String webUrl = "http://localhost:7070";
    // correos de las personas administradoras: reciben el aviso de entrega no satisfactoria
    private List<String> adminEmails = new ArrayList<>();
}
