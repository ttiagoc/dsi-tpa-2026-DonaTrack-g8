package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Periodo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el Requerimiento: "Necesidades de entidades beneficiarias"
 * Las entidades beneficiarias registran necesidades (extraordinarias o
 * recurrentes).
 * Una necesidad se satisface cuando la suma de las cantidades de las donaciones
 * asignadas cumple con la cantidad requerida.
 * - Extraordinaria: se satisface cuando se llega a la cantidad total, sin
 * importar fechas.
 * - Recurrente: solo se cuentan las donaciones del perÃ­odo actual (diario,
 * semanal, mensual, anual).
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

    private Donacion crearDonacion(Long cantidad, LocalDateTime fecha) {
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
            necesidad = new Necesidad(subcategoria, tipo, "Necesito 100 paquetes de fideos para el comedor", 100L);
        }

        @Test
        @DisplayName("Sin donaciones, la necesidad NO estÃ¡ satisfecha")
        void sinDonacionesNoSatisfecha() {
            assertFalse(necesidad.getSatisfecha());
        }

        @Test
        @DisplayName("Con donaciones insuficientes, NO estÃ¡ satisfecha")
        void conDonacionesInsuficientes() {
            necesidad.asignarDonacion(crearDonacion(30L, LocalDateTime.now()));
            necesidad.asignarDonacion(crearDonacion(20L, LocalDateTime.now()));
            assertFalse(necesidad.getSatisfecha(),
                    "50 de 100 no alcanza");
        }

        @Test
        @DisplayName("Con donaciones exactas, SÃ  estÃ¡ satisfecha")
        void conDonacionesExactas() {
            necesidad.asignarDonacion(crearDonacion(60L, LocalDateTime.now()));
            necesidad.asignarDonacion(crearDonacion(40L, LocalDateTime.now()));
            assertTrue(necesidad.getSatisfecha(),
                    "60 + 40 = 100, iguala la meta");
        }

        @Test
        @DisplayName("Con donaciones que superan la meta, SÃ  estÃ¡ satisfecha")
        void conDonacionesExcedentes() {
            necesidad.asignarDonacion(crearDonacion(80L, LocalDateTime.now()));
            necesidad.asignarDonacion(crearDonacion(50L, LocalDateTime.now()));
            assertTrue(necesidad.getSatisfecha(),
                    "80 + 50 = 130 > 100");
        }

        @Test
        @DisplayName("Donaciones de cualquier fecha cuentan (sin importar antigÃ¼edad)")
        void donacionesAntiguasCuentan() {
            necesidad.asignarDonacion(crearDonacion(50L, LocalDateTime.now().minusYears(2)));
            necesidad.asignarDonacion(crearDonacion(50L, LocalDateTime.now().minusMonths(6)));
            assertTrue(necesidad.getSatisfecha(),
                    "Para extraordinarias no importa la fecha");
        }

        @Test
        @DisplayName("Una sola donaciÃ³n grande satisface la necesidad")
        void unaSolaDonacionGrande() {
            necesidad.asignarDonacion(crearDonacion(100L, LocalDateTime.now()));
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
        @DisplayName("PerÃ­odo SEMANAL")
        class PeriodoSemanal {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.SEMANAL);
                necesidad = new Necesidad(subcategoria, tipo, "50 kg de fideos por semana", 50L);
            }

            @Test
            @DisplayName("Sin donaciones, NO estÃ¡ satisfecha")
            void sinDonaciones() {
                assertFalse(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donaciones de esta semana se cuentan")
            void donacionesDeEstaSemana() {
                necesidad.asignarDonacion(crearDonacion(30L, LocalDateTime.now()));
                necesidad.asignarDonacion(crearDonacion(25L, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha(),
                        "30 + 25 = 55 >= 50");
            }

            @Test
            @DisplayName("Donaciones de semanas anteriores NO se cuentan")
            void donacionesDeSemanasPasadas() {
                necesidad.asignarDonacion(crearDonacion(100L, LocalDateTime.now().minusWeeks(3)));
                assertFalse(necesidad.getSatisfecha(),
                        "La donaciÃ³n de hace 3 semanas no cuenta para el perÃ­odo semanal");
            }

            @Test
            @DisplayName("Mezcla de donaciones actuales y pasadas: solo cuentan las actuales")
            void mezclaActualesYPasadas() {
                necesidad.asignarDonacion(crearDonacion(40L, LocalDateTime.now().minusWeeks(2)));
                necesidad.asignarDonacion(crearDonacion(30L, LocalDateTime.now()));
                assertFalse(necesidad.getSatisfecha(),
                        "Solo las 30 de esta semana cuentan, no alcanza a 50");
            }
        }

        @Nested
        @DisplayName("PerÃ­odo DIARIO")
        class PeriodoDiario {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.DIARIO);
                necesidad = new Necesidad(subcategoria, tipo, "10 kg de fideos por dÃ­a", 10L);
            }

            @Test
            @DisplayName("DonaciÃ³n de hoy cuenta")
            void donacionDeHoy() {
                necesidad.asignarDonacion(crearDonacion(10L, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("DonaciÃ³n de ayer NO cuenta")
            void donacionDeAyer() {
                necesidad.asignarDonacion(crearDonacion(100L, LocalDateTime.now().minusDays(1)));
                assertFalse(necesidad.getSatisfecha());
            }
        }

        @Nested
        @DisplayName("PerÃ­odo MENSUAL")
        class PeriodoMensual {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.MENSUAL);
                necesidad = new Necesidad(subcategoria, tipo, "200 kg de fideos por mes", 200L);
            }

            @Test
            @DisplayName("Donaciones de este mes cuentan")
            void donacionesDeEsteMes() {
                necesidad.asignarDonacion(crearDonacion(150L, LocalDateTime.now()));
                necesidad.asignarDonacion(crearDonacion(60L, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donaciones del mes pasado NO cuentan")
            void donacionesDelMesPasado() {
                necesidad.asignarDonacion(crearDonacion(200L, LocalDateTime.now().minusMonths(1)));
                assertFalse(necesidad.getSatisfecha());
            }
        }

        @Nested
        @DisplayName("PerÃ­odo ANUAL")
        class PeriodoAnual {

            private Necesidad necesidad;

            @BeforeEach
            void setUp() {
                NecesidadRecurrente tipo = new NecesidadRecurrente();
                tipo.setPeriodo(Periodo.ANUAL);
                necesidad = new Necesidad(subcategoria, tipo, "1000 kg de fideos por aÃ±o", 1000L);
            }

            @Test
            @DisplayName("Donaciones de este aÃ±o cuentan")
            void donacionesDeEsteAnio() {
                necesidad.asignarDonacion(crearDonacion(600L, LocalDateTime.now()));
                necesidad.asignarDonacion(crearDonacion(500L, LocalDateTime.now()));
                assertTrue(necesidad.getSatisfecha());
            }

            @Test
            @DisplayName("Donaciones del aÃ±o pasado NO cuentan")
            void donacionesDelAnioPasado() {
                necesidad.asignarDonacion(crearDonacion(2000L, LocalDateTime.now().minusYears(1)));
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
        @DisplayName("Necesidad sin TipoNecesidad asignado no estÃ¡ satisfecha")
        void sinTipoNecesidad() {
            Necesidad necesidad = new Necesidad(subcategoria, null, "Sin tipo", 10L);
            assertFalse(necesidad.getSatisfecha());
        }

        @Test
        @DisplayName("recibirDonacion agrega la donaciÃ³n a la lista de asignadas")
        void recibirDonacionAgrega() {
            NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
            Necesidad necesidad = new Necesidad(subcategoria, tipo, "Prueba", 10L);

            Donacion donacion = crearDonacion(5L, LocalDateTime.now());
            necesidad.asignarDonacion(donacion);

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
                    "ONG Solidaria", "Av. Corrientes 1234", tel, List.of(email));

            NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
            Necesidad necesidad = new Necesidad(subcategoria, tipo, "Fideos", 50L);

            entidad.registrarNecesidad(necesidad);

            assertEquals(1, entidad.getNecesidades().size());
            assertSame(necesidad, entidad.getNecesidades().getFirst());
        }

        @Test
        @DisplayName("Confirmar entrega cambia el estado de la donaciÃ³n a ENTREGADA")
        void confirmarEntregaCambiaEstado() {
            Email email = new Email();
            email.setValor("test@ong.org");
            Telefono tel = new Telefono();
            tel.setValor("+54 11 1234-5678");

            EntidadBeneficiaria entidad = new EntidadBeneficiaria(
                    "ONG Solidaria", "Av. Corrientes 1234", tel, List.of(email));

            Donacion donacion = crearDonacion(10L, LocalDateTime.now());
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());

            entidad.confirmarEntrega(donacion);
            assertEquals(TipoEstadoDonacion.ENTREGADA, donacion.estadoActual());
        }
    }
}
