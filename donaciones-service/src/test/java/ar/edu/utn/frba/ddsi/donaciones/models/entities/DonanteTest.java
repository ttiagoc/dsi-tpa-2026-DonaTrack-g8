package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;

@DisplayName("Donantes y flujo completo de donacion")
class DonanteTest {

    private Subcategoria subcategoriaFideos;
    private Subcategoria subcategoriaSillas;

    @BeforeEach
    void setUp() {
        Categoria catAlimentos = new Categoria("Alimentos", true, false, null);
        Categoria catMuebles = new Categoria("Muebles", false, true, null);

        subcategoriaFideos = new Subcategoria("Fideos", catAlimentos);
        subcategoriaSillas = new Subcategoria("Sillas", catMuebles);
    }

    @Nested
    @DisplayName("Clase Donante (Persona Humana / Persona Juridica)")
    class DonanteCoreTests {

        @Test
        @DisplayName("Un donante nuevo tiene lista de donaciones vacia")
        void donanteNuevoTieneListaVacia() {
            Donante donante = new PersonaHumana();
            assertNotNull(donante.getDonaciones());
            assertTrue(donante.getDonaciones().isEmpty());
        }

        @Test
        @DisplayName("Agregar donacion incrementa la lista y asigna al donante en las segmentadas")
        void agregarDonacion() {
            Donante donante = new PersonaHumana();

            Bien fideos = new Bien(null, null, 30L, null, subcategoriaFideos, null, LocalDate.of(2026, 12, 1));
            RegistroDonacion registro = new RegistroDonacion("Primera donacion", LocalDateTime.now(), List.of(fideos),
                    null);

            registro.segmentarDonacion();
            donante.agregarDonacion(registro);

            assertEquals(1, donante.getDonaciones().size());
            assertNotNull(registro.getDonacionesSegmentadas().getFirst().getDonante());
            assertEquals(donante, registro.getDonacionesSegmentadas().getFirst().getDonante());
        }

        @Test
        @DisplayName("Se pueden agregar multiples donaciones")
        void agregarMultiplesDonaciones() {
            Donante donante = new PersonaHumana();

            for (int i = 0; i < 5; i++) {
                RegistroDonacion registro = new RegistroDonacion();
                registro.setDescripcion("DonaciÃ³n " + i);
                registro.setFecha(LocalDateTime.now());
                donante.agregarDonacion(registro);
            }

            assertEquals(5, donante.getDonaciones().size());
        }

        @Test
        @DisplayName("Una PersonaHumana puede tener contactos y un contacto predeterminado")
        void contactosYPredeterminado() {
            Email email = new Email("ana@mail.com");
            Telefono telefono = new Telefono("+54 11 5555-5555");

            ArrayList<MedioContacto> contactos = new ArrayList<MedioContacto>();
            contactos.add(email);
            contactos.add(telefono);

            PersonaHumana humana = new PersonaHumana(null, null, contactos, email, "Ana", "Perez", null, null, null,
                    null);

            assertEquals(2, humana.getContactos().size());
            assertEquals("ana@mail.com", ((Email) humana.getContactoPredeterminado()).getValor());
        }

        @Test
        @DisplayName("Una PersonaJuridica puede tener contactos, predeterminado y representantes")
        void contactosYRepresentantesJuridica() {
            PersonaJuridica juridica = new PersonaJuridica();
            juridica.setRazonSocial("Empresa Solidaria S.A.");

            Email email = new Email();
            email.setValor("contacto@empresa.com");

            juridica.getContactos().add(email);
            juridica.setContactoPredeterminado(email);

            Representante rep = new Representante();
            rep.setNombre("Juan");
            rep.setApellido("Perez");
            rep.setCorreo(email);

            juridica.setRepresentantes(java.util.List.of(rep));

            assertEquals(1, juridica.getContactos().size());
            assertEquals("contacto@empresa.com", ((Email) juridica.getContactoPredeterminado()).getValor());
            assertEquals(1, juridica.getRepresentantes().size());
            assertEquals("Juan", juridica.getRepresentantes().getFirst().getNombre());
        }
    }

    @Nested
    @DisplayName("Flujo completo: Donante -> RegistroDonacion -> Segmentacion -> Necesidad")
    class FlujoCompleto {

        @Test
        @DisplayName("Flujo end-to-end: un donante dona bienes, se segmentan, y satisfacen una necesidad")
        void flujoEndToEnd() {
            Donante donante = new PersonaJuridica();
            ((PersonaJuridica) donante).setRazonSocial("Arcos Plateados S.A.");

            Bien fideos1 = new Bien();
            fideos1.setSubcategoria(subcategoriaFideos);
            fideos1.setCantidad(30L);
            fideos1.setFechaVencimiento(java.time.LocalDate.of(2026, 12, 1));

            Bien fideos2 = new Bien();
            fideos2.setSubcategoria(subcategoriaFideos);
            fideos2.setCantidad(20L);
            fideos2.setFechaVencimiento(java.time.LocalDate.of(2026, 12, 1));

            Bien sillaNueva = new Bien();
            sillaNueva.setSubcategoria(subcategoriaSillas);
            sillaNueva.setCantidad(5L);
            sillaNueva.setEstadoBien(EstadoBien.NUEVO);

            RegistroDonacion registro = new RegistroDonacion();
            registro.setDescripcion("DonaciÃ³n de alimentos y muebles");
            registro.setFecha(LocalDateTime.now());
            registro.setBienes(List.of(fideos1, fideos2, sillaNueva));

            registro.segmentarDonacion();

            assertEquals(2, registro.getDonacionesSegmentadas().size());

            donante.agregarDonacion(registro);
            assertEquals(1, donante.getDonaciones().size());

            NecesidadExtraordinaria tipoExtra = new NecesidadExtraordinaria();
            Necesidad necesidadFideos = new Necesidad(subcategoriaFideos, tipoExtra,
                    "Necesitamos fideos para el comedor", 50L);

            Donacion donacionFideos = registro.getDonacionesSegmentadas().stream()
                    .filter(d -> d.getSubcategoria().getNombre().equals("Fideos"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(50.0, donacionFideos.cantidadBienesRecibidos(),
                    "La donaciÃ³n agrupada de fideos deberÃ­a tener 30 + 20 = 50");
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacionFideos.estadoActual());

            necesidadFideos.asignarDonacion(donacionFideos);
            assertTrue(necesidadFideos.estaSatisfecha(),
                    "50 fideos satisfacen la necesidad de 50");
        }

        @Test
        @DisplayName("Un donante puede tener mÃºltiples registros de donaciÃ³n, cada uno segmentado independientemente")
        void multiplesRegistrosSegmentadosIndependientemente() {
            Donante donante = new PersonaHumana();

            Bien fideos = new Bien();
            fideos.setSubcategoria(subcategoriaFideos);
            fideos.setCantidad(3L);
            fideos.setFechaVencimiento(java.time.LocalDate.of(2026, 12, 1));

            RegistroDonacion registro1 = new RegistroDonacion();
            registro1.setDescripcion("Primera donaciÃ³n");
            registro1.setFecha(LocalDateTime.now());
            registro1.setBienes(List.of(fideos));
            registro1.segmentarDonacion();
            donante.agregarDonacion(registro1);

            Bien silla = new Bien();
            silla.setSubcategoria(subcategoriaSillas);
            silla.setCantidad(2L);
            silla.setEstadoBien(EstadoBien.USADO);

            RegistroDonacion registro2 = new RegistroDonacion();
            registro2.setDescripcion("Segunda donaciÃ³n");
            registro2.setFecha(LocalDateTime.now());
            registro2.setBienes(List.of(silla));
            registro2.segmentarDonacion();
            donante.agregarDonacion(registro2);

            assertEquals(2, donante.getDonaciones().size());
            assertEquals(1, registro1.getDonacionesSegmentadas().size());
            assertEquals(1, registro2.getDonacionesSegmentadas().size());
        }
    }
}
