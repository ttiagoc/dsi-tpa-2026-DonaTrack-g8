package ar.edu.utn.frba.ddsi.logistica.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.security.AutenticacionHandler;

@DisplayName("RestTemplate de logistica con la clave de servicio")
class RestTemplateConfigTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer servidor;

    @BeforeEach
    void setUp() {
        RestLogisticaConfig config = new RestLogisticaConfig();
        config.setDonacionesUrl("http://donaciones:8080/api");
        restTemplate = new RestTemplateConfig().restTemplate(config, "clave-de-test");
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("Las llamadas a donaciones llevan el header X-Api-Key")
    void agregaLaClaveADonaciones() {
        servidor.expect(requestTo("http://donaciones:8080/api/donaciones/1"))
                .andExpect(header(AutenticacionHandler.HEADER_API_KEY, "clave-de-test"))
                .andRespond(withSuccess());

        restTemplate.getForObject("http://donaciones:8080/api/donaciones/1", String.class);

        servidor.verify();
    }

    @Test
    @DisplayName("Las llamadas a otros servicios no reciben la clave")
    void noMandaLaClaveAOtrosDestinos() {
        servidor.expect(requestTo("http://notificaciones:8082/api/notificaciones"))
                .andExpect(request -> assertFalse(
                        request.getHeaders().containsHeader(AutenticacionHandler.HEADER_API_KEY)))
                .andRespond(withSuccess());

        restTemplate.postForObject("http://notificaciones:8082/api/notificaciones", "{}", Void.class);

        servidor.verify();
    }
}
