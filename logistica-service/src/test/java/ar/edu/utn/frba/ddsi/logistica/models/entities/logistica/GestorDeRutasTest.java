package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.ConfirmacionEntregaExitosaRequest;

@DisplayName("Tests de GestorDeRutas")
class GestorDeRutasTest {

    private RestTemplate restTemplate;
    private RestLogisticaConfig properties;
    private ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventManagerLogistica eventManager;
    private GestorDeRutas gestorDeRutas;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        properties = mock(RestLogisticaConfig.class);
        eventManager = mock(ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventManagerLogistica.class);

        when(properties.getDonacionesUrl()).thenReturn("http://localhost:8080/api");

        gestorDeRutas = new GestorDeRutas(restTemplate, properties, eventManager);
    }

    @Test
    @DisplayName("Debería iniciar ruta y notificar al otro microservicio")
    void iniciarRuta() {
        Long rutaId = 1L;
        Ruta ruta = mock(Ruta.class);
        when(ruta.getId()).thenReturn(rutaId);

        Parada parada = mock(Parada.class);
        when(parada.getOrden()).thenReturn(1);
        when(parada.getDestino()).thenReturn("Dir 1");
        when(parada.getEntidadId()).thenReturn(100L);
        when(parada.getDonacionIds()).thenReturn(List.of(200L, 300L));
        when(ruta.getParadas()).thenReturn(List.of(parada));

        URI url1 = UriComponentsBuilder.fromUriString("http://localhost:8080/api/donaciones/200/estado").build()
                .toUri();
        URI url2 = UriComponentsBuilder.fromUriString("http://localhost:8080/api/donaciones/300/estado").build()
                .toUri();
        EstadoDonacionRequest payload = new EstadoDonacionRequest(
                "EN_TRASLADO",
                "La donación se encuentra en camino a su destino.");

        gestorDeRutas.iniciarRuta(ruta);

        verify(restTemplate, times(1)).put(eq(url1), eq(payload));
        verify(restTemplate, times(1)).put(eq(url2), eq(payload));
    }

    @Test
    @DisplayName("Debería confirmar entrega exitosa enviando el evento al donaciones-service")
    void confirmarEntregaExitosa() {
        Long entidadId = 100L;

        Camion camion = new Camion("ABC123D", 20.0, 3.0, 1000.0, null);

        Parada parada = mock(Parada.class);
        when(parada.getOrden()).thenReturn(1);
        when(parada.getDestino()).thenReturn("Dir 1");
        when(parada.getEntidadId()).thenReturn(entidadId);
        when(parada.getDonacionIds()).thenReturn(List.of(200L, 300L));

        Ruta ruta = new Ruta(LocalDate.now(), camion, List.of(parada));

        URI urlPost = UriComponentsBuilder
                .fromUriString("http://localhost:8080/api/donaciones/recepciones")
                .build().toUri();

        gestorDeRutas.confirmarEntregaExitosa(parada, ruta);

        verify(restTemplate, times(1)).postForObject(eq(urlPost), any(ConfirmacionEntregaExitosaRequest.class),
                eq(Void.class));
    }

}
