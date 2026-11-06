package ar.edu.utn.frba.ddsi.logistica.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Chofer;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.GeocodificadorCaba;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.GestorDeRutas;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.MonitorDeRutas;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;
import ar.edu.utn.frba.ddsi.logistica.services.impl.RutaServiceImpl;

@DisplayName("Tests de RutaService: recorrido del chofer, GPS y finalización automática")
class RutaServiceTest {

    private RutaRepository rutaRepository;
    private CamionRepository camionRepository;
    private GestorDeRutas gestorDeRutas;
    private RutaServiceImpl rutaService;

    @BeforeEach
    void setUp() {
        rutaRepository = mock(RutaRepository.class);
        camionRepository = mock(CamionRepository.class);
        gestorDeRutas = mock(GestorDeRutas.class);
        rutaService = new RutaServiceImpl(rutaRepository, camionRepository, gestorDeRutas, mock(MonitorDeRutas.class));
    }

    @Test
    @DisplayName("Iniciar recorrido ubica al camión en el depósito, cada parada actualiza GPS y al resolver todas finaliza la ruta")
    void flujoRecorridoChoferCompleto() {
        Camion camion = new Camion("AD 842 KP", 28.0, 2.9, 4500.0, new Chofer("Carlos", "Gómez"));
        camion.setId(2L);

        Parada p1 = new Parada();
        p1.setId(11L);
        p1.setOrden(1);
        p1.setDestino("Av. San Martín 2341, Paternal, CABA");
        p1.setEntidadId(10L);
        p1.setDonacionIds(List.of(101L));

        Parada p2 = new Parada();
        p2.setId(12L);
        p2.setOrden(2);
        p2.setDestino("Rivadavia 5100, Caballito, CABA");
        p2.setEntidadId(15L);
        p2.setDonacionIds(List.of(102L));

        Ruta ruta = new Ruta(LocalDate.now(), camion, List.of(p1, p2));
        ruta.setId(7L);

        when(rutaRepository.findById(7L)).thenReturn(Optional.of(ruta));
        when(rutaRepository.findAll()).thenReturn(List.of(ruta));

        // El camión se inicializa en el depósito central
        assertNotNull(camion.getUbicacion());
        assertEquals(GeocodificadorCaba.DEPOSITO_LAT, camion.getUbicacion().getLatitud(), 0.0001);

        // Si el camión tenía una ubicación previa arbitraria (ej. telemetría vieja)
        camion.actualizarUbicacion(new ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ubicacion(-34.6280, -58.4560, 30.0));

        // Antes de iniciar, no se puede confirmar parada
        assertThrows(BusinessException.class, () -> rutaService.confirmarEntregaExitosa(7L, 11L));

        // Obtener ruta activa para el camión devuelve la PLANIFICADA con coordenadas del depósito
        Optional<RutaResponse> activaPlanificada = rutaService.obtenerRutaActivaPorCamion(2L);
        assertTrue(activaPlanificada.isPresent());
        assertEquals(EstadoRuta.PLANIFICADA, activaPlanificada.get().estado());
        assertEquals(GeocodificadorCaba.DEPOSITO_LAT, activaPlanificada.get().camionLatitud(), 0.0001);

        // Iniciar recorrido inicializa GPS en Depósito Central
        rutaService.actualizarEstado(7L, "EN_TRASLADO");
        assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
        assertNotNull(camion.getUbicacion());
        assertEquals(GeocodificadorCaba.DEPOSITO_LAT, camion.getUbicacion().getLatitud(), 0.0001);
        assertEquals(GeocodificadorCaba.DEPOSITO_LAT, rutaService.obtenerPorId(7L).camionLatitud(), 0.0001);
        verify(gestorDeRutas).iniciarRuta(ruta);

        // Confirmar parada 1 mueve el camión a Paternal y mantiene ruta EN_TRASLADO
        rutaService.confirmarEntregaExitosa(7L, 11L);
        assertEquals(EstadoParada.ENTREGADA, p1.getEstado());
        assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
        double[] coordsP1 = GeocodificadorCaba.ubicar(p1.getDestino(), p1.getEntidadId());
        assertEquals(coordsP1[0], camion.getUbicacion().getLatitud(), 0.0001);
        assertEquals(coordsP1[0], rutaService.obtenerPorId(7L).camionLatitud(), 0.0001);
        verify(gestorDeRutas).confirmarEntregaExitosa(p1, ruta);

        // Reportar fallo en parada 2 mueve el camión a Caballito, propaga motivo y finaliza la ruta automáticamente
        rutaService.registrarEntregaNoRecibida(7L, 12L, "Entidad cerrada por refacciones");
        assertEquals(EstadoParada.NO_RECIBIDA, p2.getEstado());
        assertEquals(EstadoRuta.FINALIZADA, ruta.getEstado());
        double[] coordsP2 = GeocodificadorCaba.ubicar(p2.getDestino(), p2.getEntidadId());
        assertEquals(coordsP2[0], camion.getUbicacion().getLatitud(), 0.0001);
        assertEquals(coordsP2[0], rutaService.obtenerPorId(7L).camionLatitud(), 0.0001);
        verify(gestorDeRutas).registrarEntregaFallida(p2, "Entidad cerrada por refacciones");
        verify(camionRepository, org.mockito.Mockito.atLeast(3)).save(any(Camion.class));
    }
}
