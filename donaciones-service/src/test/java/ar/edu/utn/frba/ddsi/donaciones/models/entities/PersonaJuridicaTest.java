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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;

@DisplayName("Tests de Persona Juridica")
class PersonaJuridicaTest {

    private PersonaJuridica crearEmpresa() {
        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);
        return new PersonaJuridica(List.of(email), email, "Empresa S.A.", "Tecnología",
                TipoOrganizacion.EMPRESA, "30-12345678-9", List.of());
    }

    @Test
    @DisplayName("Debe poder instanciarse correctamente con todos sus datos")
    void instanciacionCorrecta() {
        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);

        PersonaJuridica empresa = new PersonaJuridica(
                new ArrayList<>(java.util.List.of(email)),
                email,
                "Empresa S.A.",
                "Tecnología",
                TipoOrganizacion.EMPRESA,
                "30-12345678-9",
                new ArrayList<>());

        assertEquals("Empresa S.A.", empresa.getRazonSocial());
        assertEquals("30-12345678-9", empresa.getCuit());
        assertEquals("Tecnología", empresa.getRubro());
        assertEquals("contacto@empresa.com", empresa.getContactoPredeterminado().getValor());
    }

    @Test
    @DisplayName("Debe poder crearse solo con los datos mínimos que trae la importación CSV")
    void instanciacionConDatosMinimos() {
        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);

        PersonaJuridica empresa = new PersonaJuridica(List.of(email), email, "Empresa S.A.", null, null,
                "30-12345678-9", List.of());

        assertEquals("Empresa S.A.", empresa.getRazonSocial());
        assertEquals("30-12345678-9", empresa.getCuit());
    }

    @Test
    @DisplayName("Debe fallar si falta la razón social")
    void fallaSinRazonSocial() {
        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class, () -> new PersonaJuridica(List.of(email), email,
                "", "Tecnología", TipoOrganizacion.EMPRESA, "30-12345678-9", List.of()));

        assertTrue(ex.getMessage().contains("razon social"));
    }

    @Test
    @DisplayName("Debe fallar si falta el CUIT")
    void fallaSinCuit() {
        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);

        Exception ex = assertThrows(BusinessException.class, () -> new PersonaJuridica(List.of(email), email,
                "Empresa S.A.", "Tecnología", TipoOrganizacion.EMPRESA, null, List.of()));

        assertTrue(ex.getMessage().contains("CUIT"));
    }

    @Test
    @DisplayName("Debe fallar si ningún medio de contacto es un email")
    void fallaSinEmail() {
        MedioContacto telefono = new MedioContacto("1144444444", TipoContacto.SMS);

        Exception ex = assertThrows(BusinessException.class, () -> new PersonaJuridica(List.of(telefono),
                telefono, "Empresa S.A.", "Tecnología", TipoOrganizacion.EMPRESA, "30-12345678-9", List.of()));

        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    @DisplayName("Debe fallar al pedir la última donación si no tiene donaciones registradas")
    void ultimaDonacionFallaSinDonaciones() {
        PersonaJuridica empresa = crearEmpresa();

        Exception ex = assertThrows(RuntimeException.class, () -> {
            empresa.getFechaUltimaDonacion();
        });

        assertTrue(ex.getMessage().contains("no tiene donaciones registradas"));
    }

    @Test
    @DisplayName("Debe devolver correctamente la fecha de la última donación")
    void obtenerUltimaDonacion() {
        PersonaJuridica empresa = crearEmpresa();

        RegistroDonacion donacionReciente = new RegistroDonacion();
        donacionReciente.setFecha(LocalDateTime.now().minusDays(1));

        empresa.agregarDonacion(donacionReciente);

        LocalDate ultimaFecha = empresa.getFechaUltimaDonacion();

        assertNotNull(ultimaFecha);
        assertEquals(LocalDateTime.now().minusDays(1).toLocalDate(), ultimaFecha);
    }
}
