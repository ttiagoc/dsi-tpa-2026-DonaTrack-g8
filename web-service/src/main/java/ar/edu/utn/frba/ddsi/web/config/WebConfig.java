package ar.edu.utn.frba.ddsi.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "web")
@Getter
@Setter
public class WebConfig {

    private Api api = new Api();
    private Sesion sesion = new Sesion();

    @Getter
    @Setter
    public static class Api {
        private String donacionesUrl;
        private String logisticaUrl;
        private String notificacionesUrl;
    }

    @Getter
    @Setter
    public static class Sesion {
        /** En la nube va en true: la cookie solo viaja por HTTPS. */
        private boolean cookieSegura;
        private int minutosInactividad = 120;
    }
}
