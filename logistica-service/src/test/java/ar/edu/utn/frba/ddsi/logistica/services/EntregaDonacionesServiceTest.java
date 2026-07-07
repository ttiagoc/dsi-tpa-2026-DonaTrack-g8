package ar.edu.utn.frba.ddsi.logistica.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.impl.EntregaDonacionesServiceImpl;

@DisplayName("Tests de EntregaDonacionesServiceImpl")
class EntregaDonacionesServiceTest {

    private RutaRepository rutaRepository;
    private RestTemplate restTemplate;
    private RestLogisticaConfig properties;
    private EntregaDonacionesServiceImpl entregaService;

    @BeforeEach
    void setUp() {
        rutaRepository = mock(RutaRepository.class);
        restTemplate = mock(RestTemplate.class);
        properties = mock(RestLogisticaConfig.class);

        when(properties.getBaseUrl()).thenReturn("http://localhost:8080");

        entregaService = new EntregaDonacionesServiceImpl(rutaRepository, restTemplate, properties);
    }

    @Test
    @DisplayName("Debe iniciar ruta y notificar al otro microservicio")
    void iniciarRuta() {
        Long rutaId = 1L;
        Ruta ruta = mock(Ruta.class);
        when(ruta.getId()).thenReturn(rutaId);

        Parada parada = mock(Parada.class);
        when(parada.getOrden()).thenReturn(1);
        when(parada.getDestino()).thenReturn("Dir 1");
        when(parada.getEntidad()).thenReturn(100L);
        when(parada.getEntregas()).thenReturn(List.of(200L, 300L));
        when(ruta.getParadas()).thenReturn(List.of(parada));

        when(rutaRepository.findById(rutaId)).thenReturn(Optional.of(ruta));

        URI urlPost = UriComponentsBuilder.fromUriString("http://localhost:8080/donaciones-service/evento/inicio-ruta")
                .build().toUri();

        entregaService.iniciarRuta(rutaId);

        verify(ruta, times(1)).iniciar();
        verify(rutaRepository, times(1)).save(ruta);
        verify(restTemplate, times(1)).postForObject(eq(urlPost), any(InicioRutaRequest.class), eq(Void.class));
    }

    @Test
    @DisplayName("Debe confirmar entrega exitosa enviando el evento al donaciones-service")
    void confirmarEntregaExitosa() {
        Long rutaId = 1L;
        Long paradaId = 1L;
        Long entidadId = 100L;

        Camion camion = new Camion("ABC123D", 20.0, 3.0, 1000.0, null);

        Parada parada = mock(Parada.class);
        when(parada.getOrden()).thenReturn(1);
        when(parada.getDestino()).thenReturn("Dir 1");
        when(parada.getEntidad()).thenReturn(entidadId);
        when(parada.getEntregas()).thenReturn(List.of(200L, 300L));

        Ruta ruta = new Ruta(LocalDate.now(), camion, List.of(parada));

        when(rutaRepository.findById(rutaId)).thenReturn(Optional.of(ruta));

        URI urlPost = UriComponentsBuilder
                .fromUriString("http://localhost:8080/donaciones-service/evento/confirmacion-entrega-exitosa")
                .build().toUri();

        entregaService.confirmarEntregaExitosa(paradaId, rutaId);

        verify(restTemplate, times(1)).postForObject(eq(urlPost), any(ConfirmacionEntregaExitosaRequest.class),
                eq(Void.class));
    }

    @Test
    @DisplayName("Debe arrojar excepcion si la ruta no existe al iniciar")
    void iniciarRutaInexistente() {
        when(rutaRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(ResourceNotFoundException.class, () -> {
            entregaService.iniciarRuta(99L);
        });

        assertTrue(ex.getMessage().contains("No se encontro una ruta"));
    }
}
