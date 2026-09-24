package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;

@DisplayName("Tests de Persona Humana")
class PersonaHumanaTest {

    private PersonaHumana crearPersona() {
        MedioContacto email = new MedioContacto("juan@perez.com", TipoContacto.EMAIL);
        return new PersonaHumana(List.of(email), email, "Juan", "Perez", null, "12345678", null, null);
    }

    @Test
    @DisplayName("Debe poder instanciarse correctamente con todos sus datos")
    void instanciacionCorrecta() {
        MedioContacto email = new MedioContacto("juan@perez.com", TipoContacto.EMAIL);

        PersonaHumana persona = new PersonaHumana(
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
    @DisplayName("Debe poder crearse solo con los datos mínimos que trae la importación CSV")
    void instanciacionConDatosMinimos() {
        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

        PersonaHumana persona = new PersonaHumana(List.of(email), email, "Ana", "", null, "12345678", null,
                null);

        assertEquals("Ana", persona.getNombre());
        assertEquals("12345678", persona.getDni());
    }

    @Test
    @DisplayName("Debe fallar si falta el nombre")
    void fallaSinNombre() {
        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class,
                () -> new PersonaHumana(List.of(email), email, " ", "Perez", null, "12345678", null, null));

        assertTrue(ex.getMessage().contains("nombre"));
    }

    @Test
    @DisplayName("Debe fallar si falta el DNI")
    void fallaSinDni() {
        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class,
                () -> new PersonaHumana(List.of(email), email, "Ana", "Perez", null, null, null, null));

        assertTrue(ex.getMessage().contains("DNI"));
    }

    @Test
    @DisplayName("Debe fallar si no tiene medios de contacto")
    void fallaSinContactos() {
        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class,
                () -> new PersonaHumana(List.of(), email, "Ana", "Perez", null, "12345678", null, null));

        assertTrue(ex.getMessage().contains("al menos un medio de contacto"));
    }

    @Test
    @DisplayName("Debe fallar si ningún medio de contacto es un email")
    void fallaSinEmail() {
        MedioContacto telefono = new MedioContacto("1155555555", TipoContacto.SMS);

        Exception ex = assertThrows(BusinessException.class,
                () -> new PersonaHumana(List.of(telefono), telefono, "Ana", "Perez", null, "12345678", null,
                        null));

        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    @DisplayName("Debe fallar si el email está vacío")
    void fallaConEmailVacio() {
        MedioContacto email = new MedioContacto("", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class,
                () -> new PersonaHumana(List.of(email), email, "Ana", "Perez", null, "12345678", null, null));

        assertTrue(ex.getMessage().contains("valor del medio de contacto"));
    }

    @Test
    @DisplayName("Debe fallar si el contacto predeterminado no es uno de sus medios de contacto")
    void fallaConPredeterminadoAjeno() {
        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);
        MedioContacto otro = new MedioContacto("otra@mail.com", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class,
                () -> new PersonaHumana(List.of(email), otro, "Ana", "Perez", null, "12345678", null, null));

        assertTrue(ex.getMessage().contains("predeterminado"));
    }

    @Test
    @DisplayName("Debe fallar al pedir la última donación si no tiene donaciones registradas")
    void ultimaDonacionFallaSinDonaciones() {
        PersonaHumana persona = crearPersona();

        Exception ex = assertThrows(RuntimeException.class, () -> {
            persona.getFechaUltimaDonacion();
        });

        assertTrue(ex.getMessage().contains("no tiene donaciones registradas"));
    }

    @Test
    @DisplayName("Debe devolver correctamente la fecha de la última donación")
    void obtenerUltimaDonacion() {
        PersonaHumana persona = crearPersona();

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
