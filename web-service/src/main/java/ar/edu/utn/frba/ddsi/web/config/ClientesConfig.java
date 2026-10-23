package ar.edu.utn.frba.ddsi.web.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ar.edu.utn.frba.ddsi.web.clients.ApiClient;
import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ClientesConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Bean
    public ObjectMapper apiObjectMapper() {
        return ApiClient.crearObjectMapper();
    }

    @Bean
    public AuthApiClient authApiClient(WebConfig config, HttpClient http, ObjectMapper mapper) {
        return new AuthApiClient(config.getApi().getDonacionesUrl(), http, mapper);
    }

    @Bean
    public DonacionesApiClient donacionesApiClient(WebConfig config, HttpClient http, ObjectMapper mapper,
            AuthApiClient auth) {
        return new DonacionesApiClient(config.getApi().getDonacionesUrl(), http, mapper, auth);
    }
}
