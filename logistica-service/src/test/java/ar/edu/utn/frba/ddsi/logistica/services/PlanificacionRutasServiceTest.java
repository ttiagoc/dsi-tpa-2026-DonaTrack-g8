package ar.edu.utn.frba.ddsi.logistica.services;

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
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.GestorPlanificacionRutas;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.services.impl.PlanificacionRutasServiceImpl;

@DisplayName("Tests de PlanificacionRutasServiceImpl")
@SuppressWarnings("unchecked")
class PlanificacionRutasServiceTest {

        private RestTemplate restTemplate;
        private RestLogisticaConfig properties;
        private GestorPlanificacionRutas gestorPlanificacionRutas;
        private CamionRepository camionRepository;
        private PlanificacionRutasServiceImpl planificacionRutasService;

        @BeforeEach
        void setUp() {
                restTemplate = mock(RestTemplate.class);
                properties = mock(RestLogisticaConfig.class);
                gestorPlanificacionRutas = mock(GestorPlanificacionRutas.class);
                camionRepository = mock(CamionRepository.class);

                when(properties.getDonacionesUrl()).thenReturn("http://localhost:8080/api");

                planificacionRutasService = new PlanificacionRutasServiceImpl(restTemplate, properties,
                                gestorPlanificacionRutas, camionRepository);
        }

        @Test
        @DisplayName("Debe delegar la planificacion al gestor cuando hay lote y camiones disponibles")
        void planificarRutasExitoso() {
                URI urlGetLote = UriComponentsBuilder
                                .fromUriString("http://localhost:8080/api/donaciones-service/donacion/asignadas")
                                .queryParam("limit", 100).build().toUri();

                List<DonacionDTO> loteSimulado = List.of(new DonacionDTO(1L, 50.0, 2.0, "Calle 123"));
                ResponseEntity<List<DonacionDTO>> responseMock = ResponseEntity.ok(loteSimulado);

                when(restTemplate.exchange(eq(urlGetLote), eq(HttpMethod.GET), eq(null),
                                any(ParameterizedTypeReference.class)))
                                .thenReturn(responseMock);

                Camion camion = new Camion("ABC123D", 20.0, 3.0, 1000.0, null);
                List<Camion> camiones = List.of(camion);
                when(camionRepository.findAllDisponibles()).thenReturn(camiones);

                planificacionRutasService.planificarRutas();

                verify(gestorPlanificacionRutas, times(1)).solicitarPlanificacion(loteSimulado, camiones);
        }

        @Test
        @DisplayName("No debe solicitar planificacion si no hay camiones disponibles")
        void planificarRutasSinCamiones() {
                URI urlGetLote = UriComponentsBuilder
                                .fromUriString("http://localhost:8080/api/donaciones-service/donacion/asignadas")
                                .queryParam("limit", 100).build().toUri();

                List<DonacionDTO> loteSimulado = List.of(new DonacionDTO(1L, 50.0, 2.0, "Calle 123"));
                ResponseEntity<List<DonacionDTO>> responseMock = ResponseEntity.ok(loteSimulado);

                when(restTemplate.exchange(eq(urlGetLote), eq(HttpMethod.GET), eq(null),
                                any(ParameterizedTypeReference.class)))
                                .thenReturn(responseMock);

                when(camionRepository.findAllDisponibles()).thenReturn(List.of());

                planificacionRutasService.planificarRutas();

                verify(gestorPlanificacionRutas, never()).solicitarPlanificacion(any(), any());
        }

        @Test
        @DisplayName("Debe notificar donaciones planificadas y desencadenar nueva planificacion")
        void ejecutarPlanificacion() {
                DireccionRequest dir1 = new DireccionRequest("Calle 1", List.of(10L, 20L));
                CamionPlanificacionRequest camionRequest = new CamionPlanificacionRequest(1L, List.of(dir1));
                EjecutarPlanificacionRequest request = new EjecutarPlanificacionRequest(List.of(camionRequest),
                                List.of());

                URI urlPostLista = UriComponentsBuilder
                                .fromUriString("http://localhost:8080/api/donaciones-service/donacion/lista-entrega")
                                .build().toUri();

                // Simular getLote() vacío para que termine rápido la cadena
                URI urlGetLote = UriComponentsBuilder
                                .fromUriString("http://localhost:8080/api/donaciones-service/donacion/asignadas")
                                .queryParam("limit", 100).build().toUri();
                ResponseEntity<List<DonacionDTO>> responseMock = ResponseEntity.ok(List.of());
                when(restTemplate.exchange(eq(urlGetLote), eq(HttpMethod.GET), eq(null),
                                any(ParameterizedTypeReference.class)))
                                .thenReturn(responseMock);

                planificacionRutasService.ejecutarPlanificacion(request);

                List<Long> expectedIdsPost = List.of(10L, 20L);
                verify(restTemplate, times(1)).postForEntity(eq(urlPostLista), eq(expectedIdsPost), eq(Void.class));
        }
}
