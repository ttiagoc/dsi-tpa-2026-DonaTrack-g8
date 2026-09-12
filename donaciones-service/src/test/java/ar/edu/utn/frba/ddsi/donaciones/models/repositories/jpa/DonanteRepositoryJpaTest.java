package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonante;

@DisplayName("Persistencia de Donante (SINGLE_TABLE)")
class DonanteRepositoryJpaTest extends PersistenciaTest {

    private final JpaDonante repositorio = new JpaDonante();

    @Test
    @DisplayName("Guarda personas humanas y juridicas en la misma tabla y las lee polimorficamente")
    void guardaYLeeAmbosSubtipos() {
        MedioContacto emailAna = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);
        PersonaHumana ana = new PersonaHumana(null, List.of(emailAna), emailAna,
                "Ana", "Perez", LocalDate.of(1990, 5, 1), "12345678", "F", "Medrano 951");

        MedioContacto emailEmpresa = new MedioContacto("contacto@arcos.com", TipoContacto.EMAIL);
        PersonaJuridica arcos = new PersonaJuridica(null, List.of(emailEmpresa), emailEmpresa,
                "Arcos Plateados S.A.", "Mobiliario", TipoOrganizacion.EMPRESA, "30-12345678-9",
                List.of(new Representante("Juan", "Gomez", new MedioContacto("juan@arcos.com", TipoContacto.EMAIL))));

        Long idAna = repositorio.save(ana).getId();
        Long idArcos = repositorio.save(arcos).getId();
        nuevoRequest();

        assertEquals(2, repositorio.findAll().size());

        Donante anaLeida = repositorio.findById(idAna).orElseThrow();
        assertInstanceOf(PersonaHumana.class, anaLeida);
        assertEquals("12345678", ((PersonaHumana) anaLeida).getDni());
        assertEquals("ana@mail.com", anaLeida.getContactoPredeterminado().getValor());

        PersonaJuridica arcosLeida = (PersonaJuridica) repositorio.findById(idArcos).orElseThrow();
        assertEquals(TipoOrganizacion.EMPRESA, arcosLeida.getTipo());
        assertEquals("juan@arcos.com", arcosLeida.getRepresentantes().get(0).getCorreo().getValor());
    }

    @Test
    @DisplayName("Busca un donante por el email de sus contactos")
    void buscaPorEmail() {
        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);
        PersonaHumana ana = new PersonaHumana(null, List.of(email), email,
                "Ana", "Perez", null, "12345678", "F", "Medrano 951");
        repositorio.save(ana);
        nuevoRequest();

        assertTrue(repositorio.buscarPorEmail("ANA@mail.com").isPresent());
        assertTrue(repositorio.buscarPorEmail("otro@mail.com").isEmpty());
    }
}
