package ar.edu.utn.frba.ddsi.logistica.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Chofer;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.impl.JpaCamion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.impl.JpaRuta;

@DisplayName("Tests de persistencia de RutaRepository (JPA)")
class RutaRepositoryJpaTest extends PersistenciaTest {

    private final JpaRuta rutaRepository = new JpaRuta();
    private final JpaCamion camionRepository = new JpaCamion();

    @Test
    @DisplayName("Guarda y recupera una ruta con paradas y coleccion de donacionIds en cascada")
    void guardaYRecuperaRutaConParadas() {
        Camion camion = camionRepository.save(
                new Camion("RT123AB", 25.0, 3.8, 9000.0, new Chofer("Esteban", "Quito")));

        Parada parada1 = new Parada(null, 1, "Av. Corrientes 1234", 101L, List.of(201L, 202L));
        Parada parada2 = new Parada(null, 2, "Av. Rivadavia 5678", 102L, List.of(301L));

        Ruta ruta = new Ruta(LocalDate.of(2026, 9, 20), camion, List.of(parada2, parada1)); // pasamos desordenadas

        Long rutaId = rutaRepository.save(ruta).getId();
        assertNotNull(rutaId);

        nuevoRequest();

        Optional<Ruta> recuperada = rutaRepository.findById(rutaId);
        assertTrue(recuperada.isPresent());
        Ruta r = recuperada.get();
        assertEquals(LocalDate.of(2026, 9, 20), r.getFecha());
        assertEquals(EstadoRuta.PLANIFICADA, r.getEstado());
        assertEquals("RT123AB", r.getCamion().getPatente());
        assertNotNull(r.getChofer());
        assertEquals("Esteban", r.getChofer().getNombre());
        assertEquals("Quito", r.getChofer().getApellido());

        // Verificamos que las paradas se persisten y se ordenan por 'orden ASC'
        assertEquals(2, r.getParadas().size());
        assertEquals(1, r.getParadas().get(0).getOrden());
        assertEquals("Av. Corrientes 1234", r.getParadas().get(0).getDestino());
        assertEquals(List.of(201L, 202L), r.getParadas().get(0).getDonacionIds());
        assertEquals(ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada.PENDIENTE, r.getParadas().get(0).getEstado());
        assertNotNull(r.getParadas().get(0).getId());

        assertEquals(2, r.getParadas().get(1).getOrden());
        assertEquals("Av. Rivadavia 5678", r.getParadas().get(1).getDestino());
        assertEquals(List.of(301L), r.getParadas().get(1).getDonacionIds());
        assertEquals(ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoParada.PENDIENTE, r.getParadas().get(1).getEstado());
        assertNotNull(r.getParadas().get(1).getId());
    }

    @Test
    @DisplayName("Busca rutas activas en estado EN_TRASLADO")
    void buscarRutasActivas() {
        Camion c1 = camionRepository.save(new Camion("CA101", 10.0, 2.0, 2000.0, new Chofer("A", "A")));
        Camion c2 = camionRepository.save(new Camion("CA102", 10.0, 2.0, 2000.0, new Chofer("B", "B")));

        Ruta rPlanificada = new Ruta(LocalDate.now(), c1, List.of());
        rutaRepository.save(rPlanificada);

        Ruta rEnTraslado = new Ruta(LocalDate.now(), c2, List.of());
        rEnTraslado.iniciar();
        rutaRepository.save(rEnTraslado);

        nuevoRequest();

        List<Ruta> activas = rutaRepository.buscarRutasActivas();
        assertEquals(1, activas.size());
        assertEquals(EstadoRuta.EN_TRASLADO, activas.get(0).getEstado());
        assertEquals("CA102", activas.get(0).getCamion().getPatente());
    }

    @Test
    @DisplayName("Busca rutas activas por camion (estado <> FINALIZADA)")
    void buscarRutasActivasPorCamion() {
        Camion c = camionRepository.save(new Camion("C999", 10.0, 2.0, 2000.0, new Chofer("C", "C")));

        Ruta r1 = new Ruta(LocalDate.now(), c, List.of()); // PLANIFICADA
        rutaRepository.save(r1);

        Ruta r2 = new Ruta(LocalDate.now().minusDays(1), c, List.of());
        r2.finalizar(); // FINALIZADA
        rutaRepository.save(r2);

        nuevoRequest();

        List<Ruta> activasPorCamion = rutaRepository.buscarRutasActivasPorCamion(c.getId());
        assertEquals(1, activasPorCamion.size());
        assertEquals(EstadoRuta.PLANIFICADA, activasPorCamion.get(0).getEstado());
    }

    @Test
    @DisplayName("Busca ruta asignada al camion o lanza IllegalStateException si no tiene ninguna")
    void buscarRutaDelCamion() {
        Camion cConRuta = camionRepository.save(new Camion("CR01", 10.0, 2.0, 2000.0, new Chofer("X", "X")));
        Camion cSinRuta = camionRepository.save(new Camion("CR02", 10.0, 2.0, 2000.0, new Chofer("Y", "Y")));

        Ruta r = new Ruta(LocalDate.now(), cConRuta, List.of());
        rutaRepository.save(r);

        nuevoRequest();

        Ruta encontrada = rutaRepository.buscarRutaDelCamion(cConRuta.getId());
        assertNotNull(encontrada);
        assertEquals("CR01", encontrada.getCamion().getPatente());

        assertThrows(IllegalStateException.class, () -> {
            rutaRepository.buscarRutaDelCamion(cSinRuta.getId());
        });
    }

    @Test
    @DisplayName("Elimina una ruta y sus paradas asociadas en cascada")
    void eliminarRutaEnCascada() {
        Camion camion = camionRepository.save(new Camion("DEL99", 10.0, 2.0, 2000.0, new Chofer("Z", "Z")));
        Parada p = new Parada(null, 1, "Dir 1", 10L, List.of(100L));
        Ruta r = rutaRepository.save(new Ruta(LocalDate.now(), camion, List.of(p)));
        Long rutaId = r.getId();

        nuevoRequest();

        assertTrue(rutaRepository.deleteById(rutaId));

        nuevoRequest();
        assertFalse(rutaRepository.findById(rutaId).isPresent());
    }
}
