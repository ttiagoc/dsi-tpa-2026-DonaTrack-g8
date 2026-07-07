package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;

@DisplayName("Tests de Persona Humana")
class PersonaHumanaTest {

    @Test
    @DisplayName("Debe poder instanciarse correctamente con todos sus datos")
    void instanciacionCorrecta() {
        MedioContacto email = new MedioContacto("juan@perez.com", TipoContacto.EMAIL);

        PersonaHumana persona = new PersonaHumana(
                1L,
                new ArrayList<>(java.util.List.of(email)),
                email,
                "Juan",
                "Perez",
                LocalDate.of(1990, 5, 10),
                "12345678",
                "Masculino",
                "Av Siempre Viva 123");

        assertEquals("Juan", persona.getNombre());
        assertEquals("Perez", persona.getApellido());
        assertEquals("12345678", persona.getDni());
        assertEquals("juan@perez.com", persona.getContactoPredeterminado().getValor());
    }

    @Test
    @DisplayName("Debe fallar al pedir la última donación si no tiene donaciones registradas")
    void ultimaDonacionFallaSinDonaciones() {
        PersonaHumana persona = new PersonaHumana();

        Exception ex = assertThrows(RuntimeException.class, () -> {
            persona.getFechaUltimaDonacion();
        });

        assertTrue(ex.getMessage().contains("no tiene donaciones registradas"));
    }

    @Test
    @DisplayName("Debe devolver correctamente la fecha de la última donación")
    void obtenerUltimaDonacion() {
        PersonaHumana persona = new PersonaHumana();

        RegistroDonacion donacionAntigua = new RegistroDonacion();
        donacionAntigua.setFecha(LocalDateTime.now().minusMonths(2));

        RegistroDonacion donacionReciente = new RegistroDonacion();
        donacionReciente.setFecha(LocalDateTime.now().minusDays(5));

        persona.agregarDonacion(donacionAntigua);
        persona.agregarDonacion(donacionReciente);

        LocalDate ultimaFecha = persona.getFechaUltimaDonacion();

        assertNotNull(ultimaFecha);
        assertEquals(LocalDateTime.now().minusDays(5).toLocalDate(), ultimaFecha);
    }
}
