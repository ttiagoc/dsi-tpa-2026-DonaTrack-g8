package ar.edu.utn.frba.ddsi.logistica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "rest")
@Getter
@Setter
public class RestLogisticaConfig {
    private String donacionesUrl;
    private String notificacionesUrl;
    private String logisticaUrl;
}
