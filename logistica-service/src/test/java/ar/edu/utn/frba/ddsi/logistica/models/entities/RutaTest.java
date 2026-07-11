package ar.edu.utn.frba.ddsi.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;

@DisplayName("Tests de Ruta")
class RutaTest {

    @Test
    @DisplayName("Debe inicializarse en estado PLANIFICADA usando el constructor")
    void crearRuta() {
        Camion camion = new Camion("ABC123D", 25.5, 3.2, 1500.0, null);
        Ruta ruta = new Ruta(LocalDate.now(), camion, new ArrayList<>());

        assertNull(ruta.getId());
        assertEquals(EstadoRuta.PLANIFICADA, ruta.getEstado());
        assertEquals(camion, ruta.getCamion());
        assertNotNull(ruta.getParadas());
        assertEquals(0, ruta.getParadas().size());
    }



    @Test
    @DisplayName("iniciar() debe mutar el estado a EN_TRASLADO")
    void iniciarRuta() {
        Ruta ruta = new Ruta(LocalDate.now(), null, new ArrayList<>());
        assertEquals(EstadoRuta.PLANIFICADA, ruta.getEstado());

        ruta.iniciar();

        assertEquals(EstadoRuta.EN_TRASLADO, ruta.getEstado());
    }

    @Test
    @DisplayName("finalizar() debe mutar el estado a FINALIZADA")
    void finalizarRuta() {
        Ruta ruta = new Ruta(LocalDate.now(), null, new ArrayList<>());
        ruta.iniciar();

        ruta.finalizar();

        assertEquals(EstadoRuta.FINALIZADA, ruta.getEstado());
    }
}
