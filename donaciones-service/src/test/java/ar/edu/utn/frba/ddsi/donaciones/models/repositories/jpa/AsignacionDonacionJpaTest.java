package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaCategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaEntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaRegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaSubcategoria;

@DisplayName("Persistencia de la asignacion de donaciones (matchmaking)")
class AsignacionDonacionJpaTest extends PersistenciaTest {

    private final JpaCategoria categorias = new JpaCategoria();
    private final JpaSubcategoria subcategorias = new JpaSubcategoria();
    private final JpaDonante donantes = new JpaDonante();
    private final JpaRegistroDonacion registros = new JpaRegistroDonacion();
    private final JpaDonacion donaciones = new JpaDonacion();
    private final JpaEntidadBeneficiaria entidades = new JpaEntidadBeneficiaria();
    private final JpaResultadoMatchmaking propuestas = new JpaResultadoMatchmaking();

    private Long idDonacion;
    private Long idComedor;
    private Long idEscuela;

    @BeforeEach
    void prepararDonacionYEntidades() {
        Categoria alimentos = categorias.save(new Categoria("Alimentos", false, true));
        Subcategoria fideos = subcategorias.save(new Subcategoria("Fideos", alimentos));

        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);
        Donante ana = donantes.save(new PersonaHumana(null, List.of(email), email,
                "Ana", "Perez", null, "12345678", "F", "Medrano 951"));

        RegistroDonacion registro = registros.save(new RegistroDonacion(ana, "Colecta de alimentos"));
        List<Bien> bienes = List.of(
                new Bien("Fideos secos", 100L, 0.5, 0.001, fideos, null, LocalDate.of(2027, 1, 1)));
        idDonacion = donaciones.saveAll(new SegmentadorDeDonacion().segmentarDonacion(registro, bienes)).get(0).getId();

        idComedor = entidades.save(new EntidadBeneficiaria("Comedor Sonrisas", "Escobar 123", "1122334455",
                List.of(new MedioContacto("comedor@org.com", TipoContacto.EMAIL)))).getId();
        idEscuela = entidades.save(new EntidadBeneficiaria("Escuela Rural 10", "Ruta 3 km 20", "1100000000",
                List.of())).getId();
        nuevoRequest();
    }

    @Test
    @DisplayName("Guarda la propuesta con sus entidades sugeridas en la tabla de union")
    void persistePropuestaConEntidadesSugeridas() {
        Donacion donacion = donaciones.findById(idDonacion).orElseThrow();
        EntidadBeneficiaria comedor = entidades.findById(idComedor).orElseThrow();
        EntidadBeneficiaria escuela = entidades.findById(idEscuela).orElseThrow();

        Long id = propuestas.save(new ResultadoMatchmaking(donacion, List.of(comedor, escuela))).getId();
        nuevoRequest();

        ResultadoMatchmaking leida = propuestas.findById(id).orElseThrow();

        assertEquals(idDonacion, leida.getDonacion().getId());
        assertEquals(EstadoPropuesta.PENDIENTE, leida.getEstado());
        assertNotNull(leida.getFechaEjecucion());
        assertEquals(List.of("Comedor Sonrisas", "Escuela Rural 10"),
                leida.getEntidadesSugeridas().stream()
                        .map(EntidadBeneficiaria::getRazonSocial)
                        .sorted()
                        .toList());
    }

    @Test
    @DisplayName("buscarPendientes ignora las propuestas ya resueltas")
    void buscaSoloLasPropuestasPendientes() {
        Donacion donacion = donaciones.findById(idDonacion).orElseThrow();
        EntidadBeneficiaria comedor = entidades.findById(idComedor).orElseThrow();

        Long idPendiente = propuestas.save(new ResultadoMatchmaking(donacion, List.of(comedor))).getId();

        ResultadoMatchmaking aceptada = new ResultadoMatchmaking(donacion, List.of(comedor));
        aceptada.setEstado(EstadoPropuesta.ACEPTADO);
        propuestas.save(aceptada);
        nuevoRequest();

        List<ResultadoMatchmaking> pendientes = propuestas.buscarPendientes();

        assertEquals(2, propuestas.findAll().size());
        assertEquals(1, pendientes.size());
        assertEquals(idPendiente, pendientes.get(0).getId());
    }

    @Test
    @DisplayName("La entidad asignada a la donacion nace nula y sobrevive al guardado")
    void asignaEntidadBeneficiariaALaDonacion() {
        Donacion recienCreada = donaciones.findById(idDonacion).orElseThrow();
        assertNull(recienCreada.getEntidadBeneficiariaAsignada());

        recienCreada.setEntidadBeneficiariaAsignada(entidades.findById(idComedor).orElseThrow());
        recienCreada.cambiarEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA, "Asignada al comedor");
        donaciones.save(recienCreada);
        nuevoRequest();

        Donacion leida = donaciones.findById(idDonacion).orElseThrow();

        assertEquals(idComedor, leida.getEntidadBeneficiariaAsignada().getId());
        assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, leida.estadoActual());
        assertEquals("Escobar 123", leida.obtenerDireccion());
    }

    @Test
    @DisplayName("Las donaciones asignadas a una necesidad quedan en su tabla de union")
    void persisteDonacionesAsignadasALaNecesidad() {
        Subcategoria fideos = subcategorias.findByNombre("Fideos").orElseThrow();
        EntidadBeneficiaria comedor = entidades.findById(idComedor).orElseThrow();
        comedor.registrarNecesidad(new Necesidad(fideos, new NecesidadRecurrente(Periodo.SEMANAL), "Fideos", 100L));
        entidades.save(comedor);
        nuevoRequest();

        Necesidad sinAsignar = entidades.findById(idComedor).orElseThrow().getNecesidades().get(0);
        assertTrue(sinAsignar.getDonacionesAsignadas().isEmpty());
        assertFalse(sinAsignar.estaSatisfecha());

        sinAsignar.asignarDonacion(donaciones.findById(idDonacion).orElseThrow());
        entidades.save(entidades.findById(idComedor).orElseThrow());
        nuevoRequest();

        Necesidad leida = entidades.findById(idComedor).orElseThrow().getNecesidades().get(0);

        assertEquals(1, leida.getDonacionesAsignadas().size());
        assertEquals(idDonacion, leida.getDonacionesAsignadas().get(0).getId());
        // 100 unidades de fideos cubren las 100 pedidas dentro del periodo semanal
        assertTrue(leida.estaSatisfecha());
    }
}
