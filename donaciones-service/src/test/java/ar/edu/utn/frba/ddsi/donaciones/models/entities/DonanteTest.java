package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el Requerimiento: "Donantes y registro de donaciones"
 *
 * Un Donante puede ser una PersonaHumana o una PersonaJuridica.
 * El Donante acumula un historial de RegistroDonacion.
 * Cada RegistroDonacion contiene bienes y se segmenta automáticamente.
 */
@DisplayName("Donantes y flujo completo de donación")
class DonanteTest {

    private Subcategoria subcategoriaFideos;
    private Subcategoria subcategoriaSillas;

    @BeforeEach
    void setUp() {
        Categoria catAlimentos = new Categoria();
        catAlimentos.setNombre("Alimentos");
        catAlimentos.setEsPerecedero(true);
        catAlimentos.setPideEstado(false);

        Categoria catMuebles = new Categoria();
        catMuebles.setNombre("Muebles");
        catMuebles.setEsPerecedero(false);
        catMuebles.setPideEstado(true);

        subcategoriaFideos = new Subcategoria();
        subcategoriaFideos.setNombre("Fideos");
        subcategoriaFideos.setCategoria(catAlimentos);

        subcategoriaSillas = new Subcategoria();
        subcategoriaSillas.setNombre("Sillas");
        subcategoriaSillas.setCategoria(catMuebles);
    }

    @Nested
    @DisplayName("Clase Donante")
    class DonanteCoreTests {

        @Test
        @DisplayName("Un donante nuevo tiene lista de donaciones vacía")
        void donanteNuevoTieneListaVacia() {
            Donante donante = new Donante();
            assertNotNull(donante.getDonaciones());
            assertTrue(donante.getDonaciones().isEmpty());
        }

        @Test
        @DisplayName("Agregar donación incrementa la lista")
        void agregarDonacion() {
            Donante donante = new Donante();

            RegistroDonacion registro = new RegistroDonacion();
            registro.setDescripcion("Primera donación");
            registro.setFecha(LocalDateTime.now());

            donante.agregarDonacion(registro);
            assertEquals(1, donante.getDonaciones().size());
        }

        @Test
        @DisplayName("Se pueden agregar múltiples donaciones")
        void agregarMultiplesDonaciones() {
            Donante donante = new Donante();

            for (int i = 0; i < 5; i++) {
                RegistroDonacion registro = new RegistroDonacion();
                registro.setDescripcion("Donación " + i);
                registro.setFecha(LocalDateTime.now());
                donante.agregarDonacion(registro);
            }

            assertEquals(5, donante.getDonaciones().size());
        }
    }

    @Nested
    @DisplayName("Flujo completo: Donante -> RegistroDonacion -> Segmentación -> Necesidad")
    class FlujoCompleto {

        @Test
        @DisplayName("Flujo end-to-end: un donante dona bienes, se segmentan, y satisfacen una necesidad")
        void flujoEndToEnd() {
            // 1. Crear donante
            Donante donante = new Donante();

            // 2. Crear bienes para la donación
            Bien fideos1 = new Bien();
            fideos1.setSubcategoria(subcategoriaFideos);
            fideos1.setCantidad(30.0);
            fideos1.setFechaVencimiento(java.time.LocalDate.of(2026, 12, 1));

            Bien fideos2 = new Bien();
            fideos2.setSubcategoria(subcategoriaFideos);
            fideos2.setCantidad(20.0);
            fideos2.setFechaVencimiento(java.time.LocalDate.of(2026, 12, 1));

            Bien sillaNueva = new Bien();
            sillaNueva.setSubcategoria(subcategoriaSillas);
            sillaNueva.setCantidad(5.0);
            sillaNueva.setEstadoBien(EstadoBien.NUEVO);

            // 3. Crear registro de donación
            RegistroDonacion registro = new RegistroDonacion();
            registro.setDescripcion("Donación de alimentos y muebles");
            registro.setFecha(LocalDateTime.now());
            registro.setBienes(List.of(fideos1, fideos2, sillaNueva));

            // 4. Segmentar
            registro.segmentarDonacion();

            // 5. Verificar segmentación: 2 donaciones (fideos se agrupan, sillas aparte)
            assertEquals(2, registro.getDonacionesSegmentadas().size());

            // 6. Registrar en el donante
            donante.agregarDonacion(registro);
            assertEquals(1, donante.getDonaciones().size());

            // 7. Crear una necesidad extraordinaria y asignar una donación
            NecesidadExtraordinaria tipoExtra = new NecesidadExtraordinaria();
            Necesidad necesidadFideos = new Necesidad(subcategoriaFideos, tipoExtra,
                "Necesitamos fideos para el comedor", 50.0);

            // Buscar la donación de fideos segmentada
            Donacion donacionFideos = registro.getDonacionesSegmentadas().stream()
                .filter(d -> d.getSubcategoria().getNombre().equals("Fideos"))
                .findFirst()
                .orElseThrow();

            assertEquals(50.0, donacionFideos.cantidadBienesRecibidos(),
                "La donación agrupada de fideos debería tener 30 + 20 = 50");
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacionFideos.estadoActual());

            // 8. Asignar a la necesidad
            necesidadFideos.recibirDonacion(donacionFideos);
            assertTrue(necesidadFideos.getSatisfecha(),
                "50 fideos satisfacen la necesidad de 50");
        }

        @Test
        @DisplayName("Un donante puede tener múltiples registros de donación, cada uno segmentado independientemente")
        void multiplesRegistrosSegmentadosIndependientemente() {
            Donante donante = new Donante();

            // Primera donación: 3 fideos
            Bien fideos = new Bien();
            fideos.setSubcategoria(subcategoriaFideos);
            fideos.setCantidad(3.0);
            fideos.setFechaVencimiento(java.time.LocalDate.of(2026, 12, 1));

            RegistroDonacion registro1 = new RegistroDonacion();
            registro1.setDescripcion("Primera donación");
            registro1.setFecha(LocalDateTime.now());
            registro1.setBienes(List.of(fideos));
            registro1.segmentarDonacion();
            donante.agregarDonacion(registro1);

            // Segunda donación: 2 sillas
            Bien silla = new Bien();
            silla.setSubcategoria(subcategoriaSillas);
            silla.setCantidad(2.0);
            silla.setEstadoBien(EstadoBien.USADO);

            RegistroDonacion registro2 = new RegistroDonacion();
            registro2.setDescripcion("Segunda donación");
            registro2.setFecha(LocalDateTime.now());
            registro2.setBienes(List.of(silla));
            registro2.segmentarDonacion();
            donante.agregarDonacion(registro2);

            // Verificaciones
            assertEquals(2, donante.getDonaciones().size());
            assertEquals(1, registro1.getDonacionesSegmentadas().size());
            assertEquals(1, registro2.getDonacionesSegmentadas().size());
        }
    }
}
