package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@DisplayName("Tests de PlanificadorDeRutas")
@SuppressWarnings("unchecked")
class PlanificadorDeRutasTest {

        private RestTemplate restTemplate;
        private RestLogisticaConfig properties;
        private GestorPlanificacionRutas gestorPlanificacionRutas;
        private CamionRepository camionRepository;
        private PlanificadorDeRutas planificadorDeRutas;

        @BeforeEach
        void setUp() {
                restTemplate = mock(RestTemplate.class);
                properties = mock(RestLogisticaConfig.class);
                gestorPlanificacionRutas = mock(GestorPlanificacionRutas.class);
                camionRepository = mock(CamionRepository.class);

                when(properties.getDonacionesUrl()).thenReturn("http://localhost:8080/api");

                planificadorDeRutas = new PlanificadorDeRutas(restTemplate, properties,
                                gestorPlanificacionRutas, camionRepository);
        }

        @Test
        @DisplayName("Debe delegar la planificacion al gestor cuando hay lote y camiones disponibles")
        void planificarRutasExitoso() {
                URI urlGetLote = UriComponentsBuilder
                                .fromUriString("http://localhost:8080/api/donaciones/estado/asignacion_realizada")
                                .queryParam("limit", 100).build().toUri();

                List<DonacionDTO> loteSimulado = List.of(new DonacionDTO(1L, 50.0, 2.0, "Calle 123"));
                ResponseEntity<List<DonacionDTO>> responseMock = ResponseEntity.ok(loteSimulado);

                when(restTemplate.exchange(eq(urlGetLote), eq(HttpMethod.GET), eq(null),
                                any(ParameterizedTypeReference.class)))
                                .thenReturn(responseMock);

                Camion camion = new Camion("ABC123D", 20.0, 3.0, 1000.0, null);
                List<Camion> camiones = List.of(camion);
                when(camionRepository.findAllDisponibles()).thenReturn(camiones);

                planificadorDeRutas.planificarRutas();

                verify(gestorPlanificacionRutas, times(1)).solicitarPlanificacion(loteSimulado, camiones);
        }

        @Test
        @DisplayName("No debe solicitar planificacion si no hay camiones disponibles")
        void planificarRutasSinCamiones() {
                URI urlGetLote = UriComponentsBuilder
                                .fromUriString("http://localhost:8080/api/donaciones/estado/asignacion_realizada")
                                .queryParam("limit", 100).build().toUri();

                List<DonacionDTO> loteSimulado = List.of(new DonacionDTO(1L, 50.0, 2.0, "Calle 123"));
                ResponseEntity<List<DonacionDTO>> responseMock = ResponseEntity.ok(loteSimulado);

                when(restTemplate.exchange(eq(urlGetLote), eq(HttpMethod.GET), eq(null),
                                any(ParameterizedTypeReference.class)))
                                .thenReturn(responseMock);

                when(camionRepository.findAllDisponibles()).thenReturn(List.of());

                planificadorDeRutas.planificarRutas();

                verify(gestorPlanificacionRutas, never()).solicitarPlanificacion(any(), any());
        }

        @Test
        @DisplayName("Debe notificar donaciones planificadas")
        void ejecutarPlanificacion() {
                DireccionRequest dir1 = new DireccionRequest("Calle 1", List.of(10L, 20L));
                CamionPlanificacionRequest camionRequest = new CamionPlanificacionRequest(1L, List.of(dir1));
                EjecutarPlanificacionRequest request = new EjecutarPlanificacionRequest(List.of(camionRequest),
                                List.of());

                URI url1 = UriComponentsBuilder.fromUriString("http://localhost:8080/api/donaciones/10/estado").build().toUri();
                URI url2 = UriComponentsBuilder.fromUriString("http://localhost:8080/api/donaciones/20/estado").build().toUri();
                EstadoDonacionRequest payload = new EstadoDonacionRequest(
                                "lista_para_entregar",
                                "Donacion lista para entregar"
                );

                planificadorDeRutas.ejecutarPlanificacion(request);

                verify(restTemplate, times(1)).put(eq(url1), eq(payload));
                verify(restTemplate, times(1)).put(eq(url2), eq(payload));
        }
}
