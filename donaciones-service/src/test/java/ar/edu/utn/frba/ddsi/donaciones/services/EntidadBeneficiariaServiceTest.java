package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ReportarNoRecibidaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.EntidadBeneficiariaServiceImpl;

@DisplayName("Tests del EntidadBeneficiariaService")
class EntidadBeneficiariaServiceTest {

    private EntidadBeneficiariaRepository entidadRepository;
    private DonacionRepository donacionRepository;
    private EventoService eventoService;
    private EntidadBeneficiariaServiceImpl entidadService;

    @BeforeEach
    void setUp() {
        entidadRepository = mock(EntidadBeneficiariaRepository.class);
        donacionRepository = mock(DonacionRepository.class);
        eventoService = mock(EventoService.class);

        entidadService = new EntidadBeneficiariaServiceImpl(entidadRepository, donacionRepository, eventoService);
    }

    @Test
    @DisplayName("Debe fallar al crear una entidad beneficiaria si faltan datos obligatorios")
    void crearEntidadFaltanDatos() {
        List<MedioContactoRequest> correos = new ArrayList<>();
        correos.add(new MedioContactoRequest("EMAIL", "entidad@org.com"));

        EntidadBeneficiariaRequest requestSinRazonSocial = new EntidadBeneficiariaRequest(
                "", "Medrano 951", "11223344", correos
        );

        Exception ex = assertThrows(BusinessException.class, () -> {
            entidadService.crear(requestSinRazonSocial);
        });

        assertTrue(ex.getMessage().contains("razon social no puede ser nula ni estar vacia"));
    }

    @Test
    @DisplayName("Debe poder crear exitosamente la entidad beneficiaria")
    void crearEntidadExitosamente() {
        List<MedioContactoRequest> correos = new ArrayList<>();
        correos.add(new MedioContactoRequest("EMAIL", "entidad@org.com"));

        EntidadBeneficiariaRequest requestValido = new EntidadBeneficiariaRequest(
                "Comedor Los Niños", "Medrano 951", "11223344", correos
        );

        // Al guardar debe retornar la misma entidad con ID 1
        when(entidadRepository.save(any(EntidadBeneficiaria.class))).thenAnswer(invocation -> {
            EntidadBeneficiaria guardada = invocation.getArgument(0);
            guardada.setId(1L);
            return guardada;
        });

        EntidadBeneficiariaResponse response = entidadService.crear(requestValido);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Comedor Los Niños", response.razonSocial());
        assertEquals("Medrano 951", response.direccion());
        
        verify(entidadRepository, times(1)).save(any(EntidadBeneficiaria.class));
    }

    @Test
    @DisplayName("Al reportar donacion no recibida, debe disparar notificacion en EventoService")
    void reportarNoRecibida() {
        Long entidadId = 1L;
        Long donacionId = 200L;
        String motivo = "El camion nunca llegó";

        EntidadBeneficiaria entidad = new EntidadBeneficiaria(
                "Comedor", "Calle Falsa 123", "123", new ArrayList<>(List.of(new MedioContacto("x@x.com", new Email())))
        );
        entidad.setId(entidadId);

        when(entidadRepository.findById(entidadId)).thenReturn(Optional.of(entidad));

        ReportarNoRecibidaRequest request = new ReportarNoRecibidaRequest(motivo);
        entidadService.reportarNoRecibida(entidadId, donacionId, request);

        // Se debió llamar a EventoService
        verify(eventoService, times(1)).notificarEntregaFallida(donacionId, motivo);
    }
    
    @Test
    @DisplayName("Debe lanzar ResourceNotFound si la entidad no existe al reportar")
    void reportarNoRecibidaEntidadNoExiste() {
        when(entidadRepository.findById(99L)).thenReturn(Optional.empty());

        ReportarNoRecibidaRequest request = new ReportarNoRecibidaRequest("Fallo");
        
        Exception ex = assertThrows(ResourceNotFoundException.class, () -> {
            entidadService.reportarNoRecibida(99L, 1L, request);
        });

        assertTrue(ex.getMessage().contains("No se encontro una entidad beneficiaria"));
    }
}
