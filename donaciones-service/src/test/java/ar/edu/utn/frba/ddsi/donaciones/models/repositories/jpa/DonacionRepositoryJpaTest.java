package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.CambioEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaCategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaRegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaSubcategoria;

@DisplayName("Persistencia de RegistroDonacion y Donacion")
class DonacionRepositoryJpaTest extends PersistenciaTest {

    private final JpaCategoria categorias = new JpaCategoria();
    private final JpaSubcategoria subcategorias = new JpaSubcategoria();
    private final JpaDonante donantes = new JpaDonante();
    private final JpaRegistroDonacion registros = new JpaRegistroDonacion();
    private final JpaDonacion donaciones = new JpaDonacion();

    private Long idDonante;
    private List<Donacion> donacionesCreadas;

    @BeforeEach
    void registrarDonacion() {
        Categoria mobiliario = categorias.save(new Categoria("Mobiliario", true, false));
        Categoria alimentos = categorias.save(new Categoria("Alimentos", false, true));
        Subcategoria sillas = subcategorias.save(new Subcategoria("Sillas", mobiliario));
        Subcategoria fideos = subcategorias.save(new Subcategoria("Fideos", alimentos));

        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);
        Donante donante = donantes.save(new PersonaHumana(List.of(email), email,
                "Ana", "Perez", null, "12345678", "F", "Medrano 951"));
        idDonante = donante.getId();

        RegistroDonacion registro = registros.save(new RegistroDonacion(donante, "Mudanza de oficina"));
        List<Bien> bienes = List.of(
                new Bien("Silla", 6L, 5.0, 0.3, sillas, EstadoBien.USADO, null),
                new Bien("Fideos secos", 100L, 0.5, 0.001, fideos, null, LocalDate.of(2027, 1, 1)));
        donante.agregarDonacion(registro);
        donantes.save(donante);

        donacionesCreadas = donaciones.saveAll(new SegmentadorDeDonacion().segmentarDonacion(registro, bienes));
        nuevoRequest();
    }

    @Test
    @DisplayName("Cada donacion queda asociada a su registro, con donante, fecha, bienes e historial")
    void persisteDonacionesSegmentadas() {
        List<Donacion> leidas = donaciones.findAll();

        assertEquals(2, leidas.size());
        for (Donacion donacion : leidas) {
            assertEquals("Mudanza de oficina", donacion.getRegistroDonacion().getDescripcion());
            assertEquals(idDonante, donacion.getDonante().getId());
            assertEquals(donacion.getRegistroDonacion().getFecha(), donacion.getFecha());
            assertEquals(1, donacion.getBienes().size());
            assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());
        }
    }

    @Test
    @DisplayName("El donante recupera sus registros y la fecha de su ultima donacion")
    void donanteConoceSuUltimaDonacion() {
        Donante donante = donantes.findById(idDonante).orElseThrow();

        assertEquals(1, donante.getDonaciones().size());
        assertEquals(LocalDate.now(), donante.getFechaUltimaDonacion());
    }

    @Test
    @DisplayName("Busca por estado actual segun el ultimo cambio del historial")
    void buscaPorEstadoActual() {
        Donacion donacion = donaciones.findById(donacionesCreadas.get(0).getId()).orElseThrow();
        donacion.cambiarEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA, "Asignada");
        donaciones.save(donacion);
        nuevoRequest();

        List<Donacion> enDeposito = donaciones.buscarPorEstado(TipoEstadoDonacion.EN_DEPOSITO);
        List<Donacion> asignadas = donaciones.buscarPorEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA);

        assertEquals(1, enDeposito.size());
        assertEquals(1, asignadas.size());
        assertEquals(donacion.getId(), asignadas.get(0).getId());
        assertEquals(2, asignadas.get(0).getHistorialEstados().size());
        assertTrue(enDeposito.stream().noneMatch(d -> d.getId().equals(donacion.getId())));
    }

    @Test
    @DisplayName("Las fotos de recepcion se guardan como coleccion propia de la donacion")
    void persisteFotosDeRecepcion() {
        Donacion donacion = donaciones.findById(donacionesCreadas.get(0).getId()).orElseThrow();
        assertTrue(donacion.getFotosRecepcion().isEmpty());

        donacion.getFotosRecepcion().add("https://fotos/recepcion-1.jpg");
        donacion.getFotosRecepcion().add("https://fotos/recepcion-2.jpg");
        donaciones.save(donacion);
        nuevoRequest();

        assertEquals(List.of("https://fotos/recepcion-1.jpg", "https://fotos/recepcion-2.jpg"),
                donaciones.findById(donacion.getId()).orElseThrow().getFotosRecepcion());
    }

    @Test
    @DisplayName("La entrega deja registrado que camion la realizo (TPA2)")
    void persistePatenteDelCamionQueEntrego() {
        Donacion donacion = donaciones.findById(donacionesCreadas.get(0).getId()).orElseThrow();
        donacion.confirmarEntrega("AB123CD", LocalDateTime.now());
        donaciones.save(donacion);
        nuevoRequest();

        Donacion leida = donaciones.findById(donacion.getId()).orElseThrow();

        assertEquals(TipoEstadoDonacion.ENTREGADA, leida.estadoActual());
        assertEquals("AB123CD", leida.patenteCamionDeLaEntrega());
        // el resto de las transiciones no lleva patente
        assertNull(leida.getHistorialEstados().get(0).getPatenteCamion());
    }

    @Test
    @DisplayName("Cada transicion agrega una fila sin reescribir las anteriores")
    void elHistorialEsDeSoloAgregado() {
        Donacion donacion = donaciones.findById(donacionesCreadas.get(0).getId()).orElseThrow();
        assertEquals(1, donacion.getHistorialEstados().size());
        // se marca la fila original directo en la base: si Hibernate reescribiera el historial, la marca se perderia
        withTransaction(() -> entityManager()
                .createNativeQuery("UPDATE cambio_estado SET justificacion = 'marca' WHERE donacion_id = ?1")
                .setParameter(1, donacion.getId())
                .executeUpdate());

        donacion.cambiarEstado(TipoEstadoDonacion.ASIGNACION_REALIZADA, "Asignada");
        donaciones.save(donacion);
        nuevoRequest();

        List<CambioEstado> historial = donaciones.findById(donacion.getId()).orElseThrow().getHistorialEstados();

        assertEquals(2, historial.size());
        assertEquals("marca", historial.get(0).getJustificacion());
        assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, historial.get(1).getEstado());
    }

    @Test
    @DisplayName("La PK del historial es la clave natural (donacion_id, orden)")
    void clavePrimariaDelHistorial() {
        List<?> columnas = entityManager().createNativeQuery(
                "SELECT k.column_name FROM information_schema.key_column_usage k "
                        + "JOIN information_schema.table_constraints t ON k.constraint_name = t.constraint_name "
                        + "WHERE t.constraint_type = 'PRIMARY KEY' AND t.table_name = 'CAMBIO_ESTADO' "
                        + "ORDER BY k.ordinal_position")
                .getResultList();

        assertEquals(List.of("DONACION_ID", "ORDEN"), columnas);
    }
}
