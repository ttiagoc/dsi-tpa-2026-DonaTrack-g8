package ar.edu.utn.frba.ddsi.logistica.models.entities.eventos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.mockito.ArgumentCaptor;
import ar.edu.utn.frba.ddsi.logistica.dto.notificacion.NotificacionRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.logistica.grpc.DonacionesGrpcClient;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;

@DisplayName("Tests de InicioRutaListener con cliente gRPC")
class InicioRutaListenerTest {

    private EventManagerLogistica eventManager;
    private DonacionesGrpcClient donacionesGrpcClient;
    private RestTemplate restTemplate;
    private RestLogisticaConfig config;
    private InicioRutaListener listener;

    @BeforeEach
    void setUp() {
        eventManager = mock(EventManagerLogistica.class);
        donacionesGrpcClient = mock(DonacionesGrpcClient.class);
        restTemplate = mock(RestTemplate.class);
        config = mock(RestLogisticaConfig.class);

        when(config.getNotificacionesUrl()).thenReturn("http://localhost:8082/api");
        when(config.getWebUrl()).thenReturn("https://donatrack.ejemplo.com/");

        listener = new InicioRutaListener(eventManager, donacionesGrpcClient, restTemplate, config);
    }

    @Test
    @DisplayName("Debe consultar contactos de entidad y donante por gRPC al iniciar ruta")
    void procesarInicioRuta() {
        Parada parada = mock(Parada.class);
        when(parada.getEntidadId()).thenReturn(10L);
        when(parada.getDonacionIds()).thenReturn(List.of(100L));

        Ruta ruta = mock(Ruta.class);
        when(ruta.getParadas()).thenReturn(List.of(parada));

        EventoInicioRuta evento = new EventoInicioRuta(ruta, "http://mapa.test");

        when(donacionesGrpcClient.obtenerContactosEntidad(10L))
                .thenReturn(List.of(new MedioContacto("entidad@test.com", TipoContacto.EMAIL)));
        when(donacionesGrpcClient.obtenerDonacion(100L))
                .thenReturn(new DonacionResponse(100L, 50L));
        when(donacionesGrpcClient.obtenerContactoDonante(50L))
                .thenReturn(new MedioContacto("donante@test.com", TipoContacto.EMAIL));

        listener.ejecutar(evento);

        verify(donacionesGrpcClient, times(1)).obtenerContactosEntidad(10L);
        verify(donacionesGrpcClient, times(1)).obtenerDonacion(100L);
        verify(donacionesGrpcClient, times(1)).obtenerContactoDonante(50L);
    }

    @Test
    @DisplayName("El link del aviso de donación en camino usa la URL pública del sitio (WEB_APP_URL), no localhost")
    void linkDelAvisoUsaLaUrlDelSitio() {
        Parada parada = mock(Parada.class);
        when(parada.getEntidadId()).thenReturn(10L);
        when(parada.getDonacionIds()).thenReturn(List.of(100L));
        Ruta ruta = mock(Ruta.class);
        when(ruta.getParadas()).thenReturn(List.of(parada));

        when(donacionesGrpcClient.obtenerContactosEntidad(10L))
                .thenReturn(List.of(new MedioContacto("entidad@test.com", TipoContacto.EMAIL)));
        when(donacionesGrpcClient.obtenerDonacion(100L)).thenReturn(new DonacionResponse(100L, 50L));
        when(donacionesGrpcClient.obtenerContactoDonante(50L))
                .thenReturn(new MedioContacto("donante@test.com", TipoContacto.EMAIL));

        // GestorDeRutas emite el evento con <web-url>/seguimiento
        listener.ejecutar(new EventoInicioRuta(ruta, "https://donatrack.ejemplo.com/seguimiento"));

        ArgumentCaptor<NotificacionRequest> avisos = ArgumentCaptor.forClass(NotificacionRequest.class);
        verify(restTemplate, timeout(2000).times(2))
                .postForObject(eq("http://localhost:8082/api/notificaciones"), avisos.capture(), eq(Void.class));
        List<String> mensajes = avisos.getAllValues().stream().map(NotificacionRequest::getMensaje).toList();
        assertTrue(mensajes.stream().anyMatch(m -> m.endsWith("https://donatrack.ejemplo.com/entidad/seguimiento")), mensajes.toString());
        assertTrue(mensajes.stream().anyMatch(m -> m.endsWith("https://donatrack.ejemplo.com/donante/seguimiento")), mensajes.toString());
        assertTrue(mensajes.stream().noneMatch(m -> m.contains("localhost")), mensajes.toString());
    }
}
