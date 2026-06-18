package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el Requerimiento: "Registrar donación de bienes"
 * Los bienes de una donación deben segmentarse automáticamente en Donaciones
 * agrupadas por subcategoría, estado del bien y fecha de vencimiento (si aplica).
 * Cada Donacion resultante arranca en estado EN_DEPOSITO.
 */
@DisplayName("Segmentación automática de donaciones (RegistroDonacion)")
class SegmentacionDonacionTest {

    private Subcategoria subcategoriaFideos;
    private Subcategoria subcategoriaLeche;
    private Subcategoria subcategoriaSillas;
    private LocalDateTime fechaDonacion;

    @BeforeEach
    void setUp() {
        fechaDonacion = LocalDateTime.of(2026, 5, 20, 10, 0);

        // Alimentos: perecedero=true, pideEstado=false
        Categoria categoriaAlimentos = new Categoria();
        categoriaAlimentos.setNombre("Alimentos");
        categoriaAlimentos.setEsPerecedero(true);
        categoriaAlimentos.setPideEstado(false);

        // Muebles: perecedero=false, pideEstado=true
        Categoria categoriaMuebles = new Categoria();
        categoriaMuebles.setNombre("Muebles");
        categoriaMuebles.setEsPerecedero(false);
        categoriaMuebles.setPideEstado(true);

        subcategoriaFideos = new Subcategoria();
        subcategoriaFideos.setNombre("Fideos");
        subcategoriaFideos.setCategoria(categoriaAlimentos);

        subcategoriaLeche = new Subcategoria();
        subcategoriaLeche.setNombre("Leche");
        subcategoriaLeche.setCategoria(categoriaAlimentos);

        subcategoriaSillas = new Subcategoria();
        subcategoriaSillas.setNombre("Sillas");
        subcategoriaSillas.setCategoria(categoriaMuebles);
    }

    // --- Helpers ---
    private Bien crearBien(Subcategoria sub, Double cantidad, EstadoBien estado, LocalDate vencimiento) {
        Bien bien = new Bien();
        bien.setSubcategoria(sub);
        bien.setCantidad(cantidad);
        bien.setEstadoBien(estado);
        bien.setFechaVencimiento(vencimiento);
        return bien;
    }

    private RegistroDonacion crearRegistroConBienes(List<Bien> bienes) {
        RegistroDonacion registro = new RegistroDonacion();
        registro.setDescripcion("Donación de prueba");
        registro.setFecha(fechaDonacion);
        registro.setBienes(bienes);
        return registro;
    }

    // --- Tests ---

    @Nested
    @DisplayName("Casos base")
    class CasosBase {

        @Test
        @DisplayName("Con lista de bienes vacía no genera donaciones")
        void conBienesVaciosNoGeneraDonaciones() {
            RegistroDonacion registro = crearRegistroConBienes(new ArrayList<>());
            registro.segmentarDonacion();
            assertTrue(registro.getDonacionesSegmentadas().isEmpty());
        }

        @Test
        @DisplayName("Con lista de bienes nula no genera donaciones")
        void conBienesNulosNoGeneraDonaciones() {
            RegistroDonacion registro = crearRegistroConBienes(null);
            registro.setBienes(null);
            registro.segmentarDonacion();
            // No debería lanzar excepción
            assertTrue(registro.getDonacionesSegmentadas().isEmpty());
        }

        @Test
        @DisplayName("Un solo bien genera exactamente una donación")
        void unSoloBienGeneraUnaDonacion() {
            Bien bien = crearBien(subcategoriaFideos, 5.0, null, LocalDate.of(2026, 12, 1));

            RegistroDonacion registro = crearRegistroConBienes(List.of(bien));
            registro.segmentarDonacion();

            assertEquals(1, registro.getDonacionesSegmentadas().size());
            Donacion donacion = registro.getDonacionesSegmentadas().getFirst();
            assertEquals(subcategoriaFideos, donacion.getSubcategoria());
            assertEquals(1, donacion.getBienes().size());
            assertEquals(5.0, donacion.cantidadBienesRecibidos());
        }
    }

    @Nested
    @DisplayName("Agrupación por subcategoría")
    class AgrupacionPorSubcategoria {

        @Test
        @DisplayName("Bienes de la misma subcategoría se agrupan en una sola donación")
        void mismaSubcategoriaSeAgrupaEnUnaDonacion() {
            LocalDate vencimiento = LocalDate.of(2026, 12, 1);
            Bien bien1 = crearBien(subcategoriaFideos, 3.0, null, vencimiento);
            Bien bien2 = crearBien(subcategoriaFideos, 7.0, null, vencimiento);

            RegistroDonacion registro = crearRegistroConBienes(List.of(bien1, bien2));
            registro.segmentarDonacion();

            assertEquals(1, registro.getDonacionesSegmentadas().size());
            Donacion donacion = registro.getDonacionesSegmentadas().getFirst();
            assertEquals(10.0, donacion.cantidadBienesRecibidos());
            assertEquals(2, donacion.getBienes().size());
        }

        @Test
        @DisplayName("Bienes de distintas subcategorías generan donaciones separadas")
        void distintasSubcategoriasGeneranDonacionesSeparadas() {
            Bien fideos = crearBien(subcategoriaFideos, 5.0, null, LocalDate.of(2026, 12, 1));
            Bien leche = crearBien(subcategoriaLeche, 10.0, null, LocalDate.of(2026, 11, 1));

            RegistroDonacion registro = crearRegistroConBienes(List.of(fideos, leche));
            registro.segmentarDonacion();

            assertEquals(2, registro.getDonacionesSegmentadas().size());
        }
    }

    @Nested
    @DisplayName("Agrupación por fecha de vencimiento (perecederos)")
    class AgrupacionPorVencimiento {

        @Test
        @DisplayName("Perecederos con distinta fecha de vencimiento generan donaciones separadas")
        void perecederosConDistintoVencimientoSeSeparan() {
            Bien fideos1 = crearBien(subcategoriaFideos, 3.0, null, LocalDate.of(2026, 6, 1));
            Bien fideos2 = crearBien(subcategoriaFideos, 4.0, null, LocalDate.of(2026, 12, 1));

            RegistroDonacion registro = crearRegistroConBienes(List.of(fideos1, fideos2));
            registro.segmentarDonacion();

            assertEquals(2, registro.getDonacionesSegmentadas().size(),
                "Fideos con diferentes fechas de vencimiento deben generar donaciones separadas");
        }

        @Test
        @DisplayName("Perecederos con la misma fecha de vencimiento se agrupan")
        void perecederosConMismoVencimientoSeAgrupan() {
            LocalDate mismaFecha = LocalDate.of(2026, 8, 15);
            Bien fideos1 = crearBien(subcategoriaFideos, 3.0, null, mismaFecha);
            Bien fideos2 = crearBien(subcategoriaFideos, 7.0, null, mismaFecha);

            RegistroDonacion registro = crearRegistroConBienes(List.of(fideos1, fideos2));
            registro.segmentarDonacion();

            assertEquals(1, registro.getDonacionesSegmentadas().size());
            assertEquals(10.0, registro.getDonacionesSegmentadas().getFirst().cantidadBienesRecibidos());
        }
    }

    @Nested
    @DisplayName("Agrupación por estado del bien (cuando la categoría pide estado)")
    class AgrupacionPorEstado {

        @Test
        @DisplayName("Bienes nuevos y usados de la misma subcategoría generan donaciones separadas si la categoría pide estado")
        void nuevosYUsadosSeSeparanSiPideEstado() {
            Bien sillaNueva = crearBien(subcategoriaSillas, 2.0, EstadoBien.NUEVO, null);
            Bien sillaUsada = crearBien(subcategoriaSillas, 3.0, EstadoBien.USADO, null);

            RegistroDonacion registro = crearRegistroConBienes(List.of(sillaNueva, sillaUsada));
            registro.segmentarDonacion();

            assertEquals(2, registro.getDonacionesSegmentadas().size(),
                "Sillas nuevas y usadas deben separarse porque 'Muebles' pideEstado=true");
        }

        @Test
        @DisplayName("Bienes con mismo estado de la misma subcategoría se agrupan")
        void mismoEstadoSeAgrupan() {
            Bien silla1 = crearBien(subcategoriaSillas, 2.0, EstadoBien.NUEVO, null);
            Bien silla2 = crearBien(subcategoriaSillas, 4.0, EstadoBien.NUEVO, null);

            RegistroDonacion registro = crearRegistroConBienes(List.of(silla1, silla2));
            registro.segmentarDonacion();

            assertEquals(1, registro.getDonacionesSegmentadas().size());
            assertEquals(6.0, registro.getDonacionesSegmentadas().getFirst().cantidadBienesRecibidos());
        }
    }

    @Nested
    @DisplayName("Escenario complejo con múltiples criterios")
    class EscenarioComplejo {

        @Test
        @DisplayName("Donación mixta con alimentos y muebles genera las donaciones correctas")
        void donacionMixtaGeneraDonacionesCorrectas() {
            LocalDate venc1 = LocalDate.of(2026, 6, 1);
            LocalDate venc2 = LocalDate.of(2026, 12, 1);

            // 2 fideos con vencimiento junio -> agrupados
            Bien fideos1 = crearBien(subcategoriaFideos, 3.0, null, venc1);
            Bien fideos2 = crearBien(subcategoriaFideos, 2.0, null, venc1);

            // 1 fideos con vencimiento diciembre -> separado
            Bien fideos3 = crearBien(subcategoriaFideos, 5.0, null, venc2);

            // 1 leche -> separado por subcategoría
            Bien leche1 = crearBien(subcategoriaLeche, 10.0, null, venc1);

            // 2 sillas nuevas -> agrupadas
            Bien silla1 = crearBien(subcategoriaSillas, 1.0, EstadoBien.NUEVO, null);
            Bien silla2 = crearBien(subcategoriaSillas, 1.0, EstadoBien.NUEVO, null);

            // 1 silla usada -> separada por estado
            Bien silla3 = crearBien(subcategoriaSillas, 1.0, EstadoBien.USADO, null);

            List<Bien> bienes = List.of(fideos1, fideos2, fideos3, leche1, silla1, silla2, silla3);
            RegistroDonacion registro = crearRegistroConBienes(bienes);
            registro.segmentarDonacion();

            // Esperamos 5 donaciones:
            // 1. Fideos venc junio (2 bienes)
            // 2. Fideos venc diciembre (1 bien)
            // 3. Leche venc junio (1 bien)
            // 4. Sillas NUEVO (2 bienes)
            // 5. Sillas USADO (1 bien)
            assertEquals(5, registro.getDonacionesSegmentadas().size(),
                "Se esperan 5 donaciones segmentadas en este escenario complejo");
        }
    }

    @Nested
    @DisplayName("Estado inicial de las donaciones segmentadas")
    class EstadoInicialDonaciones {

        @Test
        @DisplayName("Todas las donaciones segmentadas arrancan en estado EN_DEPOSITO")
        void donacionesArrancanEnDeposito() {
            Bien fideos = crearBien(subcategoriaFideos, 5.0, null, LocalDate.of(2026, 12, 1));
            Bien silla = crearBien(subcategoriaSillas, 2.0, EstadoBien.NUEVO, null);

            RegistroDonacion registro = crearRegistroConBienes(List.of(fideos, silla));
            registro.segmentarDonacion();

            for (Donacion donacion : registro.getDonacionesSegmentadas()) {
                assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual(),
                    "Cada donación segmentada debe arrancar EN_DEPOSITO");
            }
        }

        @Test
        @DisplayName("Las donaciones segmentadas tienen historial de estados con exactamente 1 entrada")
        void donacionesTienenUnEstadoEnHistorial() {
            Bien bien = crearBien(subcategoriaFideos, 5.0, null, LocalDate.of(2026, 12, 1));

            RegistroDonacion registro = crearRegistroConBienes(List.of(bien));
            registro.segmentarDonacion();

            Donacion donacion = registro.getDonacionesSegmentadas().getFirst();
            assertEquals(1, donacion.getHistorialEstados().size());
        }

        @Test
        @DisplayName("La fecha de la donación segmentada es la fecha del registro")
        void fechaDonacionEsLaDelRegistro() {
            Bien bien = crearBien(subcategoriaFideos, 5.0, null, LocalDate.of(2026, 12, 1));

            RegistroDonacion registro = crearRegistroConBienes(List.of(bien));
            registro.segmentarDonacion();

            assertEquals(fechaDonacion, registro.getDonacionesSegmentadas().getFirst().getFecha());
        }
    }
}
