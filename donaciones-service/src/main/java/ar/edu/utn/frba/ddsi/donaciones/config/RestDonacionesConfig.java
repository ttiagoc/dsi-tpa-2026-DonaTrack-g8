package ar.edu.utn.frba.ddsi.donaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "rest")
@Data
public class RestDonacionesConfig {
    private String notificacionesUrl;
}
