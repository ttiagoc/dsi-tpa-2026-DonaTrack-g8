package ar.edu.utn.frba.ddsi.logistica.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.impl.MonitoreoServiceImpl;

@DisplayName("Tests de MonitoreoServiceImpl")
class MonitoreoServiceTest {

    private CamionRepository camionRepository;
    private RutaRepository rutaRepository;
    private MonitoreoServiceImpl monitoreoService;

    @BeforeEach
    void setUp() {
        camionRepository = mock(CamionRepository.class);
        rutaRepository = mock(RutaRepository.class);
        monitoreoService = new MonitoreoServiceImpl(camionRepository, rutaRepository);
    }

    @Test
    @DisplayName("Debe actualizar ubicacion de camion si la ruta esta EN_TRASLADO")
    void actualizarUbicacionCamionExitoso() {
        String patente = "ABC123D";
        Camion camion = spy(new Camion(patente, 20.0, 3.0, 1000.0, null));
        doReturn(10L).when(camion).getId();

        Ruta ruta = spy(new Ruta(LocalDate.now(), camion, List.of()));
        doReturn(100L).when(ruta).getId();
        ruta.iniciar(); // Pone el estado en EN_TRASLADO

        when(camionRepository.findByPatente(patente)).thenReturn(Optional.of(camion));
        when(rutaRepository.buscarRutaDelCamion(camion.getId())).thenReturn(ruta);

        UbicacionRequest request = new UbicacionRequest(-34.6037, -58.3816, 60.5);

        monitoreoService.actualizarUbicacionCamion(patente, request);

        assertEquals(-34.6037, camion.getUbicacion().getLatitud());
        assertEquals(-58.3816, camion.getUbicacion().getLongitud());
        assertEquals(60.5, camion.getUbicacion().getVelocidad());
        verify(camionRepository, times(1)).save(camion);
    }

    @Test
    @DisplayName("Falla al actualizar ubicacion si la ruta no esta EN_TRASLADO")
    void actualizarUbicacionCamionRutaInactiva() {
        String patente = "ABC123D";
        Camion camion = spy(new Camion(patente, 20.0, 3.0, 1000.0, null));
        doReturn(10L).when(camion).getId();

        Ruta ruta = spy(new Ruta(LocalDate.now(), camion, List.of()));
        doReturn(100L).when(ruta).getId();
        // Estado inicial es PLANIFICADA

        when(camionRepository.findByPatente(patente)).thenReturn(Optional.of(camion));
        when(rutaRepository.buscarRutaDelCamion(camion.getId())).thenReturn(ruta);

        UbicacionRequest request = new UbicacionRequest(-34.6037, -58.3816, 60.5);

        Exception ex = assertThrows(BusinessException.class, () -> {
            monitoreoService.actualizarUbicacionCamion(patente, request);
        });

        assertTrue(ex.getMessage().contains("El camión no tiene ninguna ruta en estado EN_TRASLADO"));
    }

    @Test
    @DisplayName("Debe obtener la ultima ubicacion por ruta")
    void obtenerUltimaUbicacionPorRuta() {
        Long rutaId = 100L;
        Camion camion = new Camion("XYZ987", 20.0, 3.0, 1000.0, null);
        Ruta ruta = spy(new Ruta(LocalDate.now(), camion, List.of()));
        doReturn(rutaId).when(ruta).getId();
        Ubicacion ubi = new Ubicacion(-34.0, -58.0, 80.0);
        camion.actualizarUbicacion(ubi);

        when(rutaRepository.findById(rutaId)).thenReturn(Optional.of(ruta));

        UbicacionResponse response = monitoreoService.obtenerUltimaUbicacionPorRuta(rutaId);

        assertEquals(-34.0, response.latitud());
        assertEquals(-58.0, response.longitud());
        assertEquals(80.0, response.velocidad());
    }

    @Test
    @DisplayName("Debe listar camiones activos mapeando rutas EN_TRASLADO")
    void obtenerCamionesActivos() {
        Camion camion = spy(new Camion("XYZ987", 20.0, 3.0, 1000.0, null));
        doReturn(1L).when(camion).getId();
        camion.actualizarUbicacion(new Ubicacion(-34.0, -58.0, 45.0));

        Parada parada = mock(Parada.class);
        when(parada.getOrden()).thenReturn(1);
        when(parada.getDestino()).thenReturn("Dir 1");

        Ruta ruta = spy(new Ruta(LocalDate.now(), camion, List.of(parada)));
        doReturn(100L).when(ruta).getId();

        when(rutaRepository.buscarRutasActivas()).thenReturn(List.of(ruta));

        List<CamionActivoResponse> activos = monitoreoService.obtenerCamionesActivos();

        assertEquals(1, activos.size());
        assertEquals(camion.getPatente(), activos.get(0).patente());
        assertEquals(-34.0, activos.get(0).latitud());
        assertEquals(-58.0, activos.get(0).longitud());
        assertEquals(1, activos.get(0).paradasPendientes().size());
    }
}
