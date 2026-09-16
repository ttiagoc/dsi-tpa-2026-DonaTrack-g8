package ar.edu.utn.frba.ddsi.logistica.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Chofer;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ubicacion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.impl.JpaCamion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.impl.JpaRuta;

@DisplayName("Tests de persistencia de CamionRepository (JPA)")
class CamionRepositoryJpaTest extends PersistenciaTest {

    private final JpaCamion camionRepository = new JpaCamion();
    private final JpaRuta rutaRepository = new JpaRuta();

    @Test
    @DisplayName("Guarda y recupera un camion con chofer y ubicacion embebidos")
    void guardaYRecuperaCamionConChoferYUbicacion() {
        Camion camion = new Camion("AB123CD", 20.0, 3.5, 8000.0, new Chofer("Juan", "Perez"));
        camion.actualizarUbicacion(new Ubicacion(-34.6037, -58.3816, 60.0));

        Long id = camionRepository.save(camion).getId();
        assertNotNull(id);

        nuevoRequest();

        Optional<Camion> recuperado = camionRepository.findById(id);
        assertTrue(recuperado.isPresent());
        Camion c = recuperado.get();
        assertEquals("AB123CD", c.getPatente());
        assertEquals(20.0, c.getCapacidadVolumen());
        assertEquals(3.5, c.getAltura());
        assertEquals(8000.0, c.getCapacidadCarga());

        assertNotNull(c.getChofer());
        assertEquals("Juan", c.getChofer().getNombre());
        assertEquals("Perez", c.getChofer().getApellido());

        assertNotNull(c.getUbicacion());
        assertEquals(-34.6037, c.getUbicacion().getLatitud());
        assertEquals(-58.3816, c.getUbicacion().getLongitud());
        assertEquals(60.0, c.getUbicacion().getVelocidad());
    }

    @Test
    @DisplayName("Busca camion por patente case-insensitive")
    void buscarPorPatente() {
        Camion camion = new Camion("XY987ZT", 15.0, 3.0, 5000.0, new Chofer("Carlos", "Gomez"));
        camionRepository.save(camion);

        nuevoRequest();

        Optional<Camion> encontrado = camionRepository.findByPatente("xy987zt");
        assertTrue(encontrado.isPresent());
        assertEquals("Carlos", encontrado.get().getChofer().getNombre());

        Optional<Camion> noEncontrado = camionRepository.findByPatente("INEXISTENTE");
        assertFalse(noEncontrado.isPresent());
    }

    @Test
    @DisplayName("Filtra camiones disponibles segun el estado de sus rutas")
    void camionesDisponibles() {
        // Disponible = sin ninguna ruta en un estado distinto de FINALIZADA (ver JpaCamion).

        // Nunca tuvo rutas.
        camionRepository.save(new Camion("DISP01", 10.0, 2.5, 3000.0, new Chofer("Ana", "Lopez")));

        Camion conRutaFinalizada = camionRepository.save(
                new Camion("FIN01", 20.0, 3.5, 6000.0, new Chofer("Martin", "Duran")));
        Camion enTraslado = camionRepository.save(
                new Camion("OCUP01", 15.0, 3.0, 4000.0, new Chofer("Pedro", "Rios")));
        Camion conRutaPlanificada = camionRepository.save(
                new Camion("PLAN01", 12.0, 2.8, 3500.0, new Chofer("Sofia", "Arce")));
        Camion conRutaCerradaYEnCurso = camionRepository.save(
                new Camion("MIX01", 18.0, 3.2, 5000.0, new Chofer("Nicolas", "Paz")));

        Ruta rutaFinalizada = new Ruta(LocalDate.now(), conRutaFinalizada, List.of());
        rutaFinalizada.finalizar(); // FINALIZADA -> vuelve a estar disponible
        rutaRepository.save(rutaFinalizada);

        Ruta rutaActiva = new Ruta(LocalDate.now(), enTraslado, List.of());
        rutaActiva.iniciar(); // EN_TRASLADO
        rutaRepository.save(rutaActiva);

        // Recien planificada, todavia no arranco: igual deja de estar disponible.
        rutaRepository.save(new Ruta(LocalDate.now(), conRutaPlanificada, List.of()));

        // Tener una ruta ya cerrada no lo libera si arrastra otra en curso.
        Ruta viejaDelMixto = new Ruta(LocalDate.now(), conRutaCerradaYEnCurso, List.of());
        viejaDelMixto.finalizar();
        rutaRepository.save(viejaDelMixto);
        Ruta actualDelMixto = new Ruta(LocalDate.now(), conRutaCerradaYEnCurso, List.of());
        actualDelMixto.iniciar();
        rutaRepository.save(actualDelMixto);

        nuevoRequest();

        List<String> disponibles = camionRepository.findAllDisponibles().stream()
                .map(Camion::getPatente)
                .sorted()
                .toList();

        // Set exacto: asi tambien falla si la consulta se vuelve demasiado permisiva.
        assertEquals(List.of("DISP01", "FIN01"), disponibles);
    }

    @Test
    @DisplayName("Elimina camion por id")
    void eliminarCamion() {
        Camion camion = camionRepository.save(
                new Camion("DEL01", 10.0, 2.5, 3000.0, new Chofer("Leo", "Messi")));
        Long id = camion.getId();

        nuevoRequest();

        assertTrue(camionRepository.deleteById(id));

        nuevoRequest();
        assertFalse(camionRepository.findById(id).isPresent());
    }
}
