package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.DireccionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;

@DisplayName("Ubicación de las paradas en el mapa")
class ParadaTest {

    @Test
    @DisplayName("Usa la ubicación real de la sede cuando la conoce")
    void usaLaUbicacionReal() {
        Parada parada = new Parada(null, 1, "Av. San Martín 2341, Paternal, CABA", 10L, List.of(1L));
        parada.setLatitud(-34.5991);
        parada.setLongitud(-58.4688);

        assertArrayEquals(new double[] { -34.5991, -58.4688 }, parada.coordenadas());
    }

    @Test
    @DisplayName("Sin ubicación real, aproxima a partir de la dirección")
    void sinUbicacionAproxima() {
        Parada parada = new Parada(null, 1, "Av. San Martín 2341, Paternal, CABA", 10L, List.of(1L));

        assertArrayEquals(GeocodificadorCaba.ubicar("Av. San Martín 2341, Paternal, CABA", 10L), parada.coordenadas());
    }

    @Test
    @DisplayName("La planificación lleva la ubicación de la entidad hasta la parada")
    void laPlanificacionLlevaLaUbicacion() {
        Camion camion = new Camion("AA 111 AA", 10.0, 2.5, 100.0, null);
        camion.setId(1L);
        DonacionDTO donacion = new DonacionDTO(101L, 40.0, 2.0, "Av. San Martín 2341, Paternal, CABA", 10L,
                "Comedor A", -34.5991, -58.4688);

        EjecutarPlanificacionRequest resultado = new GestorPlanificacionRutas(null)
                .calcularPlanificacion(List.of(donacion), List.of(camion));

        DireccionRequest direccion = resultado.camiones().get(0).direcciones().get(0);
        assertEquals(-34.5991, direccion.latitud());
        assertEquals(-58.4688, direccion.longitud());
    }
}
