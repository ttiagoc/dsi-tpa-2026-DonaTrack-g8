package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.grpc.DonacionesGrpcClient;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@DisplayName("Tests de PlanificadorDeRutas con cliente gRPC")
class PlanificadorDeRutasTest {

    private DonacionesGrpcClient donacionesGrpcClient;
    private GestorPlanificacionRutas gestorPlanificacionRutas;
    private CamionRepository camionRepository;
    private RutaRepository rutaRepository;
    private PlanificadorDeRutas planificadorDeRutas;

    @BeforeEach
    void setUp() {
        donacionesGrpcClient = mock(DonacionesGrpcClient.class);
        gestorPlanificacionRutas = mock(GestorPlanificacionRutas.class);
        camionRepository = mock(CamionRepository.class);
        rutaRepository = mock(RutaRepository.class);

        planificadorDeRutas = new PlanificadorDeRutas(donacionesGrpcClient,
                gestorPlanificacionRutas, camionRepository, rutaRepository);
    }

    @Test
    @DisplayName("Debe delegar la planificacion al gestor cuando hay lote y camiones disponibles")
    void planificarRutasExitoso() {
        List<DonacionDTO> loteSimulado = List.of(new DonacionDTO(1L, 50.0, 2.0, "Calle 123"));
        when(donacionesGrpcClient.listarPorEstado("asignacion_realizada", 100, 0))
                .thenReturn(loteSimulado);

        Camion camion = new Camion("ABC123D", 20.0, 3.0, 1000.0, null);
        List<Camion> camiones = List.of(camion);
        when(camionRepository.findAllDisponibles()).thenReturn(camiones);

        planificadorDeRutas.planificarRutas();

        verify(gestorPlanificacionRutas, times(1)).solicitarPlanificacion(loteSimulado, camiones);
    }

    @Test
    @DisplayName("No debe solicitar planificacion si no hay camiones disponibles")
    void planificarRutasSinCamiones() {
        List<DonacionDTO> loteSimulado = List.of(new DonacionDTO(1L, 50.0, 2.0, "Calle 123"));
        when(donacionesGrpcClient.listarPorEstado("asignacion_realizada", 100, 0))
                .thenReturn(loteSimulado);

        when(camionRepository.findAllDisponibles()).thenReturn(List.of());

        planificadorDeRutas.planificarRutas();

        verify(gestorPlanificacionRutas, never()).solicitarPlanificacion(any(), any());
    }

    @Test
    @DisplayName("Debe persistir rutas/paradas y notificar donaciones planificadas vía gRPC")
    void ejecutarPlanificacion() {
        Camion camion = new Camion("ABC123D", 20.0, 3.0, 1000.0, null);
        camion.setId(1L);
        when(camionRepository.findById(1L)).thenReturn(java.util.Optional.of(camion));

        DireccionRequest dir1 = new DireccionRequest("Calle 1", List.of(10L, 20L), 99L);
        CamionPlanificacionRequest camionRequest = new CamionPlanificacionRequest(1L, List.of(dir1));
        EjecutarPlanificacionRequest request = new EjecutarPlanificacionRequest(
                List.of(camionRequest),
                List.of(30L, 40L));

        planificadorDeRutas.ejecutarPlanificacion(request);

        verify(rutaRepository, times(1)).save(any(Ruta.class));
        verify(donacionesGrpcClient, times(1)).actualizarEstado(eq(10L), eq("LISTA_PARA_ENTREGAR"), any());
        verify(donacionesGrpcClient, times(1)).actualizarEstado(eq(20L), eq("LISTA_PARA_ENTREGAR"), any());
    }

    @Test
    @DisplayName("GestorPlanificacionRutas agrupa donaciones por entidad y respeta peso y volumen del camión")
    void calcularPlanificacionAgrupaYRespetaCapacidad() {
        GestorPlanificacionRutas gestorReal = new GestorPlanificacionRutas(null, "http://localhost/noop");

        Camion c1 = new Camion("AA 111 AA", 10.0, 2.5, 100.0, null);
        c1.setId(1L);
        Camion c2 = new Camion("BB 222 BB", 10.0, 2.5, 100.0, null);
        c2.setId(2L);

        DonacionDTO d1 = new DonacionDTO(101L, 40.0, 2.0, "Av. San Martín 2341, Paternal, CABA", 10L, "Comedor A");
        DonacionDTO d2 = new DonacionDTO(102L, 35.0, 1.5, "Av. San Martín 2341, Paternal, CABA", 10L, "Comedor A");
        DonacionDTO d3 = new DonacionDTO(103L, 50.0, 3.0, "Belgrano 450, Balvanera, CABA", 12L, "Hogar B");
        DonacionDTO d4 = new DonacionDTO(104L, 90.0, 4.0, "Rivadavia 5100, Caballito, CABA", 15L, "Merendero C");

        EjecutarPlanificacionRequest resultado = gestorReal.calcularPlanificacion(
                List.of(d1, d2, d3, d4), List.of(c1, c2));

        assertEquals(2, resultado.camiones().size());
        // Camión 1 agrupa d1 y d2 en una única parada hacia la entidad 10 (75 kg <= 100 kg)
        CamionPlanificacionRequest planC1 = resultado.camiones().get(0);
        assertEquals(1L, planC1.id());
        assertEquals(1, planC1.direcciones().size());
        assertEquals(10L, planC1.direcciones().get(0).entidadId());
        assertEquals(List.of(101L, 102L), planC1.direcciones().get(0).donacionesIds());

        // d3 (50 kg) pasa al Camión 2; d4 (90 kg) excede lo restante en ambos y queda sin asignar
        CamionPlanificacionRequest planC2 = resultado.camiones().get(1);
        assertEquals(2L, planC2.id());
        assertEquals(List.of(103L), planC2.direcciones().get(0).donacionesIds());
        assertEquals(List.of(104L), resultado.donacionesSinAsignar());
    }
}
