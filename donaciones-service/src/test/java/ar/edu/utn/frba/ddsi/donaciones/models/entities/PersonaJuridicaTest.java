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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;

@DisplayName("Tests de Persona Juridica")
class PersonaJuridicaTest {

    @Test
    @DisplayName("Debe poder instanciarse correctamente con todos sus datos")
    void instanciacionCorrecta() {
        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);

        PersonaJuridica empresa = new PersonaJuridica(
                2L,
                new ArrayList<>(java.util.List.of(email)),
                email,
                "Empresa S.A.",
                "Tecnología",
                "Sociedad Anónima",
                "30-12345678-9",
                new ArrayList<>());

        assertEquals("Empresa S.A.", empresa.getRazonSocial());
        assertEquals("30-12345678-9", empresa.getCuit());
        assertEquals("Tecnología", empresa.getRubro());
        assertEquals("contacto@empresa.com", empresa.getContactoPredeterminado().getValor());
    }

    @Test
    @DisplayName("Debe fallar al pedir la última donación si no tiene donaciones registradas")
    void ultimaDonacionFallaSinDonaciones() {
        PersonaJuridica empresa = new PersonaJuridica();

        Exception ex = assertThrows(RuntimeException.class, () -> {
            empresa.getFechaUltimaDonacion();
        });

        assertTrue(ex.getMessage().contains("no tiene donaciones registradas"));
    }

    @Test
    @DisplayName("Debe devolver correctamente la fecha de la última donación")
    void obtenerUltimaDonacion() {
        PersonaJuridica empresa = new PersonaJuridica();

        RegistroDonacion donacionReciente = new RegistroDonacion();
        donacionReciente.setFecha(LocalDateTime.now().minusDays(1));

        empresa.agregarDonacion(donacionReciente);

        LocalDate ultimaFecha = empresa.getFechaUltimaDonacion();

        assertNotNull(ultimaFecha);
        assertEquals(LocalDateTime.now().minusDays(1).toLocalDate(), ultimaFecha);
    }
}
