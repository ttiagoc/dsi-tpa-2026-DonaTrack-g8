package ar.edu.utn.frba.ddsi.logistica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.security.AutenticacionHandler;

@Configuration
public class RestTemplateConfig {

    /**
     * Las llamadas a donaciones-service van sin usuario: se identifican con la clave compartida entre
     * servicios (rol SISTEMA). El header solo se agrega a los requests hacia donaciones, para no mandar
     * la clave a otros destinos.
     */
    @Bean
    public RestTemplate restTemplate(RestLogisticaConfig config,
            @Value("${seguridad.service-api-key:}") String serviceApiKey) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String donacionesUrl = config.getDonacionesUrl();
            if (!serviceApiKey.isBlank() && donacionesUrl != null
                    && request.getURI().toString().startsWith(donacionesUrl)) {
                request.getHeaders().set(AutenticacionHandler.HEADER_API_KEY, serviceApiKey);
            }
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
