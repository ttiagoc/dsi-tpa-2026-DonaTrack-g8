package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;

@DisplayName("Tests de la entidad Donacion")
class DonacionTest {

    private Bien bienBase;
    private LocalDateTime fechaCreacion;

    @BeforeEach
    void setUp() {
        Categoria catMuebles = new Categoria("Muebles", true, false);
        Subcategoria subSillas = new Subcategoria("Sillas", catMuebles);

        // Peso = 2.5 kg, Volumen = 0.5 m3, Cantidad = 4
        bienBase = new Bien("Silla de madera", 4L, 2.5, 0.5, subSillas, EstadoBien.NUEVO, null);
        fechaCreacion = LocalDateTime.now();
    }

    @Test
    @DisplayName("Debe inicializarse correctamente con un bien base y en estado EN_DEPOSITO")
    void inicializacionDonacion() {
        Donacion donacion = new Donacion(bienBase, fechaCreacion);

        assertEquals(1, donacion.getBienes().size());
        assertEquals(bienBase.getSubcategoria(), donacion.getSubcategoria());
        assertEquals(EstadoBien.NUEVO, donacion.getEstadoBienes());

        assertEquals(1, donacion.getHistorialEstados().size());
        assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());
        assertEquals("Ingreso al depósito por segmentación automática",
                donacion.getHistorialEstados().getFirst().getJustificacion());
    }

    @Test
    @DisplayName("Debe permitir agregar múltiples bienes y calcular métricas totales")
    void agregarBienesYCalcularTotales() {
        Donacion donacion = new Donacion(bienBase, fechaCreacion);

        Categoria catMuebles = new Categoria("Muebles", true, false);
        Subcategoria subMesas = new Subcategoria("Mesas", catMuebles);

        // Peso = 10.0 kg, Volumen = 1.5 m3, Cantidad = 1
        Bien mesa = new Bien("Mesa de comedor", 1L, 10.0, 1.5, subMesas, EstadoBien.NUEVO, null);
        donacion.agregarBien(mesa);

        assertEquals(2, donacion.getBienes().size());

        // Peso total: (4 * 2.5) + (1 * 10.0) = 10.0 + 10.0 = 20.0
        assertEquals(20.0, donacion.calcularPesoTotal(), 0.01);

        // Volumen total: (4 * 0.5) + (1 * 1.5) = 2.0 + 1.5 = 3.5
        assertEquals(3.5, donacion.calcularVolumenTotal(), 0.01);

        // Cantidad total: 4 + 1 = 5
        assertEquals(5.0, donacion.cantidadBienesRecibidos());
    }

    @Test
    @DisplayName("Debe permitir cambiar de estado manteniendo el historial (Trazabilidad)")
    void trazabilidadCambioDeEstado() {
        Donacion donacion = new Donacion(bienBase, fechaCreacion);

        donacion.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO, "Inicia traslado a la entidad");

        assertEquals(2, donacion.getHistorialEstados().size());
        assertEquals(TipoEstadoDonacion.EN_TRASLADO, donacion.estadoActual());
        assertEquals("Inicia traslado a la entidad", donacion.getHistorialEstados().getLast().getJustificacion());

        donacion.confirmarEntrega();

        assertEquals(3, donacion.getHistorialEstados().size());
        assertEquals(TipoEstadoDonacion.ENTREGADA, donacion.estadoActual());
        assertEquals("Entregado", donacion.getHistorialEstados().getLast().getJustificacion());
    }

    @Test
    @DisplayName("Debe verificar correctamente si se encuentra dentro del periodo actual")
    void verificaPeriodo() {
        LocalDateTime ahora = LocalDateTime.now();
        Donacion donacionReciente = new Donacion(bienBase, ahora);

        assertTrue(donacionReciente.estaDentroDelPeriodoActual(Periodo.MENSUAL));
        assertTrue(donacionReciente.estaDentroDelPeriodoActual(Periodo.ANUAL));

        LocalDateTime haceUnAnio = ahora.minusYears(1).minusMonths(1);
        Donacion donacionAntigua = new Donacion(bienBase, haceUnAnio);

        assertFalse(donacionAntigua.estaDentroDelPeriodoActual(Periodo.MENSUAL));
        assertFalse(donacionAntigua.estaDentroDelPeriodoActual(Periodo.ANUAL));
    }
}
