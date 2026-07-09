package ar.edu.utn.frba.ddsi.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Chofer;

@DisplayName("Tests de Camion")
class CamionTest {

    @Test
    @DisplayName("Debe crear un camion con el constructor completo correctamente")
    void crearCamion() {
        Chofer chofer = new Chofer("Juan", "Perez");

        Camion camion = new Camion("ABC123D", 25.5, 3.2, 1500.0, chofer);

        assertNull(camion.getId()); // Por defecto en constructor es null
        assertEquals("ABC123D", camion.getPatente());
        assertEquals(25.5, camion.getCapacidadVolumen());
        assertEquals(3.2, camion.getAltura());
        assertEquals(1500.0, camion.getCapacidadCarga());
        assertEquals(chofer, camion.getChofer());
    }
}
