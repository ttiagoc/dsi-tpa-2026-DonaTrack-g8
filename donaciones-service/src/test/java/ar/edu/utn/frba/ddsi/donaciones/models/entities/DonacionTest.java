package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;

@DisplayName("Ciclo de vida de una Donación")
class DonacionTest {

    private Subcategoria subcategoria;
    private Bien bienBase;

    @BeforeEach
    void setUp() {
        Categoria categoria = new Categoria("Alimentos", false, true, null);
        subcategoria = new Subcategoria("Fideos", categoria);
        bienBase = new Bien(null, null, 10L, null, subcategoria, null, LocalDate.of(2026, 12, 1));
    }

    @Nested
    @DisplayName("Creación e inicialización")
    class Creacion {

        @Test
        @DisplayName("Una donación nueva tiene estado EN_DEPOSITO")
        void donacionNuevaEnDeposito() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());
        }

        @Test
        @DisplayName("Se hereda la subcategoría del bien base")
        void heredaSubcategoria() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertEquals(subcategoria, donacion.getSubcategoria());
        }

        @Test
        @DisplayName("Se hereda la fecha de vencimiento del bien base")
        void heredaFechaVencimiento() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertEquals(LocalDate.of(2026, 12, 1), donacion.getFechaVencimiento());
        }

        @Test
        @DisplayName("Se hereda el estado del bien base")
        void heredaEstadoBien() {
            bienBase.setEstadoBien(EstadoBien.USADO);
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertEquals(EstadoBien.USADO, donacion.getEstadoBienes());
        }

        @Test
        @DisplayName("El bien base se agrega automáticamente a la lista de bienes")
        void bienBaseSeAgrega() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertEquals(1, donacion.getBienes().size());
            assertSame(bienBase, donacion.getBienes().getFirst());
        }

        @Test
        @DisplayName("El historial arranca con exactamente un estado")
        void historialTieneUnEstado() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertEquals(1, donacion.getHistorialEstados().size());
        }
    }

    @Nested
    @DisplayName("Agregar bienes")
    class AgregarBienes {

        @Test
        @DisplayName("Se pueden agregar bienes a la donación")
        void agregarBienSumaBienes() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());

            Bien otroBien = new Bien(null, null, 5L, null, subcategoria, null, null);

            donacion.agregarBien(otroBien);

            assertEquals(2, donacion.getBienes().size());
            assertEquals(15.0, donacion.cantidadBienesRecibidos());
        }
    }

    @Nested
    @DisplayName("Cantidad de bienes recibidos")
    class CantidadBienes {

        @Test
        @DisplayName("Calcula correctamente la suma de cantidades de todos los bienes")
        void sumaCorrectamente() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now()); // 10.0

            Bien bien2 = new Bien(null, null, 5L, null, subcategoria, null, null);
            donacion.agregarBien(bien2);

            Bien bien3 = new Bien(null, null, 3L, null, subcategoria, null, null);
            donacion.agregarBien(bien3);

            assertEquals(18L, donacion.cantidadBienesRecibidos());
        }

        @Test
        @DisplayName("Un bien con cantidad null se trata como 0")
        void bienConCantidadNullEsCero() {
            Bien bienSinCantidad = new Bien(null, null, null, null, subcategoria, null, null);

            Donacion donacion = new Donacion(bienSinCantidad, LocalDateTime.now());
            assertEquals(0L, donacion.cantidadBienesRecibidos());
        }
    }

    @Nested
    @DisplayName("Confirmar entrega")
    class ConfirmarEntrega {

        @Test
        @DisplayName("Confirmar entrega cambia el estado a ENTREGADA")
        void confirmarEntregaCambiaEstado() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            donacion.confirmarEntrega();
            assertEquals(TipoEstadoDonacion.ENTREGADA, donacion.estadoActual());
        }

        @Test
        @DisplayName("Confirmar entrega agrega un segundo estado al historial")
        void confirmarEntregaAgregaAlHistorial() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            donacion.confirmarEntrega();
            assertEquals(2, donacion.getHistorialEstados().size());
        }

        @Test
        @DisplayName("El primer estado del historial sigue siendo EN_DEPOSITO")
        void primerEstadoSigueEnDeposito() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            donacion.confirmarEntrega();
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO,
                    donacion.getHistorialEstados().getFirst().getEstado());
        }
    }

    @Nested
    @DisplayName("Período actual (filtro temporal para necesidades recurrentes)")
    class PeriodoActual {

        @Test
        @DisplayName("Donación de hoy está dentro del período DIARIO")
        void donacionHoyEstaDentroDePeriodoDiario() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertTrue(donacion.estaDentroDelPeriodoActual(Periodo.DIARIO));
        }

        @Test
        @DisplayName("Donación de ayer NO está dentro del período DIARIO")
        void donacionAyerNoEstaDentroDePeriodoDiario() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now().minusDays(1));
            assertFalse(donacion.estaDentroDelPeriodoActual(Periodo.DIARIO));
        }

        @Test
        @DisplayName("Donación de esta semana está dentro del período SEMANAL")
        void donacionEstaSemanaEstaDentroDePeriodoSemanal() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertTrue(donacion.estaDentroDelPeriodoActual(Periodo.SEMANAL));
        }

        @Test
        @DisplayName("Donación de hace 2 semanas NO está dentro del período SEMANAL")
        void donacionHaceDosSemanas() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now().minusWeeks(2));
            assertFalse(donacion.estaDentroDelPeriodoActual(Periodo.SEMANAL));
        }

        @Test
        @DisplayName("Donación de este mes está dentro del período MENSUAL")
        void donacionEsteMesEstaDentroDePeriodoMensual() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertTrue(donacion.estaDentroDelPeriodoActual(Periodo.MENSUAL));
        }

        @Test
        @DisplayName("Donación del mes pasado NO está dentro del período MENSUAL")
        void donacionMesPasado() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now().minusMonths(1));
            assertFalse(donacion.estaDentroDelPeriodoActual(Periodo.MENSUAL));
        }

        @Test
        @DisplayName("Donación de este año está dentro del período ANUAL")
        void donacionEsteAnioEstaDentro() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertTrue(donacion.estaDentroDelPeriodoActual(Periodo.ANUAL));
        }

        @Test
        @DisplayName("Donación del año pasado NO está dentro del período ANUAL")
        void donacionAnioPasado() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now().minusYears(1));
            assertFalse(donacion.estaDentroDelPeriodoActual(Periodo.ANUAL));
        }

        @Test
        @DisplayName("Con período null retorna false")
        void periodoNullRetornaFalse() {
            Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
            assertFalse(donacion.estaDentroDelPeriodoActual(null));
        }
    }
}
