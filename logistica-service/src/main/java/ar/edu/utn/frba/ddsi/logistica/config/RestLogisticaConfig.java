package ar.edu.utn.frba.ddsi.logistica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "rest")
@Data
public class RestLogisticaConfig {
    private String donacionesUrl;
    private String notificacionesUrl;
}
