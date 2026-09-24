package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.DonanteServiceImpl;

@DisplayName("Tests del DonanteService")
class DonanteServiceTest {

    private DonanteRepository donanteRepository;
    private DonanteServiceImpl donanteService;
    private PersonaJuridica empresa;

    @BeforeEach
    void setUp() {
        donanteRepository = mock(DonanteRepository.class);
        donanteService = new DonanteServiceImpl(donanteRepository);

        MedioContacto email = new MedioContacto("contacto@empresa.com", TipoContacto.EMAIL);
        empresa = new PersonaJuridica(List.of(email), email, "Empresa S.A.", "Tecnología",
                TipoOrganizacion.EMPRESA, "30-12345678-9", List.of());
        empresa.setId(5L);

        when(donanteRepository.findById(5L)).thenReturn(Optional.of(empresa));
        when(donanteRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("Actualizar una persona jurídica modifica el donante existente en lugar de crear uno nuevo")
    void actualizarPersonaJuridicaModificaElExistente() {
        PersonaJuridicaRequest request = new PersonaJuridicaRequest("Empresa Renovada S.A.", null, "ONG", null,
                null, null, null);

        DonanteResponse response = donanteService.actualizarPersonaJuridica(5L, request);

        verify(donanteRepository).save(empresa);
        assertEquals(5L, response.id());
        assertEquals("Empresa Renovada S.A.", empresa.getRazonSocial());
        assertEquals(TipoOrganizacion.ONG, empresa.getTipo());
        assertEquals("Tecnología", empresa.getRubro());
        assertEquals("30-12345678-9", empresa.getCuit());
    }

    @Test
    @DisplayName("Actualizar como persona humana un donante jurídico informa que no existe")
    void actualizarPersonaHumanaConIdDeJuridica() {
        PersonaHumanaRequest request = new PersonaHumanaRequest("Ana", null, null, null, null, null, null, null);

        assertThrows(ResourceNotFoundException.class, () -> donanteService.actualizarPersonaHumana(5L, request));
        verify(donanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar los contactos dejando al donante sin email falla y no guarda")
    void actualizarContactosSinEmailFalla() {
        MedioContactoRequest telefono = new MedioContactoRequest("telefono", "1144444444");
        PersonaJuridicaRequest request = new PersonaJuridicaRequest(null, null, null, null, null,
                List.of(telefono), telefono);

        assertThrows(BusinessException.class, () -> donanteService.actualizarPersonaJuridica(5L, request));
        verify(donanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un donante sin donaciones se puede eliminar")
    void eliminarDonanteSinDonaciones() {
        assertTrue(donanteService.eliminar(5L));
        verify(donanteRepository).deleteById(5L);
    }

    @Test
    @DisplayName("Un donante con donaciones registradas no se puede eliminar")
    void eliminarDonanteConDonacionesFalla() {
        empresa.agregarDonacion(new RegistroDonacion());

        Exception ex = assertThrows(BusinessException.class, () -> donanteService.eliminar(5L));

        assertTrue(ex.getMessage().contains("donaciones registradas"));
        verify(donanteRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar un donante inexistente devuelve false")
    void eliminarDonanteInexistente() {
        assertFalse(donanteService.eliminar(99L));
        verify(donanteRepository, never()).deleteById(any());
    }
}
