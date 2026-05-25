package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.Telefono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el Requerimiento: "Necesidades de entidades beneficiarias"
 * Las entidades beneficiarias registran necesidades (extraordinarias o recurrentes).
 * Una necesidad se satisface cuando la suma de las cantidades de las donaciones
 * asignadas cumple con la cantidad requerida.
 * - Extraordinaria: se satisface cuando se llega a la cantidad total, sin importar fechas.
 * - Recurrente: solo se cuentan las donaciones del período actual (diario, semanal, mensual, anual).
 */
@DisplayName("Necesidades de Entidades Beneficiarias")
class NecesidadTest {

    private Subcategoria subcategoria;

    @BeforeEach
    void setUp() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Alimentos");
        categoria.setEsPerecedero(true);
        categoria.setPideEstado(false);

        subcategoria = new Subcategoria();
        subcategoria.setNombre("Fideos");
        subcategoria.setCategoria(categoria);
    }

    private Donacion crearDonacion(double cantidad, LocalDateTime fecha) {
        Bien bien = new Bien();
        bien.setSubcategoria(subcategoria);
        bien.setCantidad(cantidad);
        return new Donacion(bien, fecha);
    }

    // -------------------------------------------------------
    // NECESIDAD EXTRAORDINARIA
    // -------------------------------------------------------

    @Nested
    @DisplayName("Necesidad Extraordinaria")
    class NecesidadExtraordinariaTests {

      private Necesidad necesidad;

        @BeforeEach
        void setUp() {
            NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
            necesidad = new Necesidad(subcategoria, tipo, "Necesito 100 paquetes de fideos para el comedor", 100.0);
        }

        @Test
        @DisplayName("Sin donaciones, la necesidad NO está satisfecha")
        void sinDonacionesNoSatisfecha() {
            assertFalse(necesidad.getSatisfecha());
        }

        @Test
        @DisplayName("Con donaciones insuficientes, NO está satisfecha")
        void conDonacionesInsuficientes() {
            necesidad.recibirDonacion(crearDonacion(30.0, LocalDateTime.now()));
            necesidad.recibirDonacion(crearDonacion(20.0, LocalDateTime.now()));
            assertFalse(necesidad.getSatisfecha(),
                "50 de 100 no alcanza");
        }

        @Test
        @DisplayName("Con donaciones exactas, SÍ está satisfecha")
        void conDonacionesExactas() {
            necesidad.recibirDonacion(crearDonacion(60.0, LocalDateTime.now()));
            necesidad.recibirDonacion(crearDonacion(40.0, LocalDateTime.now()));
            assertTrue(necesidad.getSatisfecha(),
                "60 + 40 = 100, iguala la meta");
        }

        @Test
        @DisplayName("Con donaciones que superan la meta, SÍ está satisfecha")
        void conDonacionesExcedentes() {
            necesidad.recibirDonacion(crearDonacion(80.0, LocalDateTime.now()));
            necesidad.recibirDonacion(crearDonacion(50.0, LocalDateTime.now()));
            assertTrue(necesidad.getSatisfecha(),
                "80 + 50 = 130 > 100");
        }

        @Test
        @DisplayName("Donaciones de cualquier fecha cuentan (sin importar antigüedad)")
        void donacionesAntiguasCuentan() {
            necesidad.recibirDonacion(crearDonacion(50.0, LocalDateTime.now().minusYears(2)));
            necesidad.recibirDonacion(crearDonacion(50.0, LocalDateTime.now().minusMonths(6)));
            assertTrue(necesidad.getSatisfecha(),
                "Para extraordinarias no importa la fecha");
        }

        @Test
        @DisplayName("Una sola donación grande satisface la necesidad")
        void unaSolaDonacionGrande() {
            necesidad.recibirDonacion(crearDonacion(100.0, LocalDateTime.now()));
            assertTrue(necesidad.getSatisfecha());
        }
    }

    // -------------------------------------------------------
    // NECESIDAD RECURRENTE
    // -------------------------------------------------------

    @Nested
    @DisplayName("Necesidad Recurrente")
    class NecesidadRecurrenteTests {

        @Nested
        @DisplayName("Período SEMANAL")
        class PeriodoSemanal {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.SEMANAL);
                necesidad = new Necesidad(subcategoria, tipo, "50 kg de fideos por semana", 50.0);
            }

            @Test
            @DisplayName("Sin donaciones, NO está satisfecha")
            void sinDonaciones() {
                assertFalse(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donaciones de esta semana se cuentan")
            void donacionesDeEstaSemana() {
                necesidad.recibirDonacion(crearDonacion(30.0, LocalDateTime.now()));
                necesidad.recibirDonacion(crearDonacion(25.0, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha(),
                    "30 + 25 = 55 >= 50");
            }

            @Test
            @DisplayName("Donaciones de semanas anteriores NO se cuentan")
            void donacionesDeSemanasPasadas() {
                necesidad.recibirDonacion(crearDonacion(100.0, LocalDateTime.now().minusWeeks(3)));
                assertFalse(necesidad.getSatisfecha(),
                    "La donación de hace 3 semanas no cuenta para el período semanal");
            }

            @Test
            @DisplayName("Mezcla de donaciones actuales y pasadas: solo cuentan las actuales")
            void mezclaActualesYPasadas() {
                necesidad.recibirDonacion(crearDonacion(40.0, LocalDateTime.now().minusWeeks(2)));
                necesidad.recibirDonacion(crearDonacion(30.0, LocalDateTime.now()));
                assertFalse(necesidad.getSatisfecha(),
                    "Solo las 30 de esta semana cuentan, no alcanza a 50");
            }
        }

        @Nested
        @DisplayName("Período DIARIO")
        class PeriodoDiario {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.DIARIO);
                necesidad = new Necesidad(subcategoria, tipo, "10 kg de fideos por día", 10.0);
            }

            @Test
            @DisplayName("Donación de hoy cuenta")
            void donacionDeHoy() {
                necesidad.recibirDonacion(crearDonacion(10.0, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donación de ayer NO cuenta")
            void donacionDeAyer() {
                necesidad.recibirDonacion(crearDonacion(100.0, LocalDateTime.now().minusDays(1)));
                assertFalse(necesidad.getSatisfecha());
            }
        }

        @Nested
        @DisplayName("Período MENSUAL")
        class PeriodoMensual {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.MENSUAL);
                necesidad = new Necesidad(subcategoria, tipo, "200 kg de fideos por mes", 200.0);
            }

            @Test
            @DisplayName("Donaciones de este mes cuentan")
            void donacionesDeEsteMes() {
                necesidad.recibirDonacion(crearDonacion(150.0, LocalDateTime.now()));
                necesidad.recibirDonacion(crearDonacion(60.0, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donaciones del mes pasado NO cuentan")
            void donacionesDelMesPasado() {
                necesidad.recibirDonacion(crearDonacion(200.0, LocalDateTime.now().minusMonths(1)));
                assertFalse(necesidad.getSatisfecha());
            }
        }

        @Nested
        @DisplayName("Período ANUAL")
        class PeriodoAnual {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.ANUAL);
                necesidad = new Necesidad(subcategoria, tipo, "1000 kg de fideos por año", 1000.0);
            }

            @Test
            @DisplayName("Donaciones de este año cuentan")
            void donacionesDeEsteAnio() {
                necesidad.recibirDonacion(crearDonacion(600.0, LocalDateTime.now()));
                necesidad.recibirDonacion(crearDonacion(500.0, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donaciones del año pasado NO cuentan")
            void donacionesDelAnioPasado() {
                necesidad.recibirDonacion(crearDonacion(2000.0, LocalDateTime.now().minusYears(1)));
                assertFalse(necesidad.getSatisfecha());
            }
        }
    }

    // -------------------------------------------------------
    // NECESIDAD SIN TIPO (edge case)
    // -------------------------------------------------------

    @Nested
    @DisplayName("Edge cases de Necesidad")
    class EdgeCases {

        @Test
        @DisplayName("Necesidad sin TipoNecesidad asignado no está satisfecha")
        void sinTipoNecesidad() {
            Necesidad necesidad = new Necesidad(subcategoria, null, "Sin tipo", 10.0);
            assertFalse(necesidad.getSatisfecha());
        }

        @Test
        @DisplayName("recibirDonacion agrega la donación a la lista de asignadas")
        void recibirDonacionAgrega() {
            NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
            Necesidad necesidad = new Necesidad(subcategoria, tipo, "Prueba", 10.0);

            Donacion donacion = crearDonacion(5.0, LocalDateTime.now());
            necesidad.recibirDonacion(donacion);

            assertEquals(1, necesidad.getDonacionesAsignadas().size());
            assertSame(donacion, necesidad.getDonacionesAsignadas().getFirst());
        }
    }

    // -------------------------------------------------------
    // ENTIDAD BENEFICIARIA
    // -------------------------------------------------------

    @Nested
    @DisplayName("Entidad Beneficiaria")
    class EntidadBeneficiariaTests {

        @Test
        @DisplayName("Puede registrar necesidades")
        void registrarNecesidad() {
            Email email = new Email();
            email.setValor("test@ong.org");
            Telefono tel = new Telefono();
            tel.setValor("+54 11 1234-5678");

            EntidadBeneficiaria entidad = new EntidadBeneficiaria(
                "ONG Solidaria", "Av. Corrientes 1234", tel, List.of(email)
            );

            NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
            Necesidad necesidad = new Necesidad(subcategoria, tipo, "Fideos", 50.0);

            entidad.registrarNecesidad(necesidad);

            assertEquals(1, entidad.getNecesidades().size());
            assertSame(necesidad, entidad.getNecesidades().getFirst());
        }

        @Test
        @DisplayName("Confirmar entrega cambia el estado de la donación a ENTREGADA")
        void confirmarEntregaCambiaEstado() {
            Email email = new Email();
            email.setValor("test@ong.org");
            Telefono tel = new Telefono();
            tel.setValor("+54 11 1234-5678");

            EntidadBeneficiaria entidad = new EntidadBeneficiaria(
                "ONG Solidaria", "Av. Corrientes 1234", tel, List.of(email)
            );

            Donacion donacion = crearDonacion(10.0, LocalDateTime.now());
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());

            entidad.confirmarEntrega(donacion);
            assertEquals(TipoEstadoDonacion.ENTREGADA, donacion.estadoActual());
        }
    }
}
