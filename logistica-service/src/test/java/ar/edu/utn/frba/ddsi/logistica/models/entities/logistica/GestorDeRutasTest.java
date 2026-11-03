package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.grpc.DonacionesGrpcClient;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventManagerLogistica;

@DisplayName("Tests de GestorDeRutas con cliente gRPC")
class GestorDeRutasTest {

    private DonacionesGrpcClient donacionesGrpcClient;
    private RestLogisticaConfig properties;
    private EventManagerLogistica eventManager;
    private GestorDeRutas gestorDeRutas;

    @BeforeEach
    void setUp() {
        donacionesGrpcClient = mock(DonacionesGrpcClient.class);
        properties = mock(RestLogisticaConfig.class);
        eventManager = mock(EventManagerLogistica.class);

        when(properties.getLogisticaUrl()).thenReturn("http://localhost:8081/api");

        gestorDeRutas = new GestorDeRutas(donacionesGrpcClient, properties, eventManager);
    }

    @Test
    @DisplayName("Debería iniciar ruta y notificar estados a Donaciones vía gRPC")
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

        gestorDeRutas.iniciarRuta(ruta);

        verify(donacionesGrpcClient, times(1)).actualizarEstado(eq(200L), eq("EN_TRASLADO"), any());
        verify(donacionesGrpcClient, times(1)).actualizarEstado(eq(300L), eq("EN_TRASLADO"), any());
    }

    @Test
    @DisplayName("Debería confirmar entrega exitosa enviando el evento a Donaciones vía gRPC")
    void confirmarEntregaExitosa() {
        Long entidadId = 100L;
        List<Long> donacionIds = List.of(200L, 300L);
        Parada parada = mock(Parada.class);
        when(parada.getEntidadId()).thenReturn(entidadId);
        when(parada.getDonacionIds()).thenReturn(donacionIds);
        when(parada.getOrden()).thenReturn(1);

        Camion camion = mock(Camion.class);
        when(camion.getPatente()).thenReturn("ABC123D");

        Ruta ruta = mock(Ruta.class);
        when(ruta.getCamion()).thenReturn(camion);

        gestorDeRutas.confirmarEntregaExitosa(parada, ruta);

        verify(donacionesGrpcClient, times(1)).confirmarEntrega(eq(entidadId), eq(donacionIds), eq("ABC123D"), any());
    }
}
