package ar.edu.utn.frba.ddsi.donaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "rest")
@Getter
@Setter
public class RestDonacionesConfig {
    private String notificacionesUrl;
}
