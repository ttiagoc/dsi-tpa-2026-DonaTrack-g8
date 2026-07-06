package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.common.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.MotorDeMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.TipoNecesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventManager;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoDonacionAsignadaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoDonacionAsignadaEntidad;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.MatchmakingServiceImpl;

@DisplayName("Tests de MatchmakingServiceImpl")
class MatchmakingServiceImplTest {

    private DonacionRepository donacionRepository;
    private EntidadBeneficiariaRepository entidadRepository;
    private ResultadoMatchmakingRepository resultadoRepository;
    private MotorDeMatchmaking motorDeMatchmaking;
    private EventManager eventManager;
    private MatchmakingServiceImpl matchmakingService;

    @BeforeEach
    void setUp() {
        donacionRepository = mock(DonacionRepository.class);
        entidadRepository = mock(EntidadBeneficiariaRepository.class);
        resultadoRepository = mock(ResultadoMatchmakingRepository.class);
        motorDeMatchmaking = mock(MotorDeMatchmaking.class);
        eventManager = mock(EventManager.class);

        matchmakingService = new MatchmakingServiceImpl(donacionRepository, entidadRepository, resultadoRepository,
                motorDeMatchmaking, eventManager);
    }

    @Test
    @DisplayName("Debe ejecutar matchmaking para donaciones en depósito y guardar los resultados")
    void procesarMatchmakingExitoso() {
        Categoria cat = new Categoria("Alimentos", false, true);
        Subcategoria sub = new Subcategoria("Fideos", cat);
        Bien bienBase = new Bien("Fideos", 1L, 0.5, 0.5, sub, EstadoBien.NUEVO, LocalDate.now().plusDays(10));
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(100L);

        List<Donacion> donaciones = List.of(donacion);
        when(donacionRepository.buscarPorEstado(TipoEstadoDonacion.EN_DEPOSITO)).thenReturn(donaciones);

        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123", new ArrayList<>());
        entidad.setId(1L);
        List<EntidadBeneficiaria> entidades = List.of(entidad);
        when(entidadRepository.findAll()).thenReturn(entidades);

        ResultadoMatchmaking resultadoSimulado = new ResultadoMatchmaking(donacion, entidades);
        when(motorDeMatchmaking.ejecutarMatchmaking(donacion, entidades)).thenReturn(resultadoSimulado);

        matchmakingService.procesarMatchmaking();

        verify(resultadoRepository, times(1)).save(resultadoSimulado);
    }

    @Test
    @DisplayName("Al aceptar propuesta, debe cambiar estado de donación y notificar")
    void aceptarPropuestaExitosamente() {
        Long propuestaId = 10L;
        Long entidadId = 1L;

        MedioContacto emailEntidad = new MedioContacto("entidad@test.com", new Email());
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123", new ArrayList<>(List.of(emailEntidad)));
        entidad.setId(entidadId);
        
        Categoria cat = new Categoria("Alimentos", false, true);
        Subcategoria sub = new Subcategoria("Fideos", cat);
        
        // Agregar necesidad a la entidad para verificar que se le asigna la donación
        TipoNecesidad mockTipoNecesidad = mock(TipoNecesidad.class);
        Necesidad necesidad = new Necesidad(sub, mockTipoNecesidad, "Necesitamos fideos", 100L);
        entidad.registrarNecesidad(necesidad);

        MedioContacto emailDonante = new MedioContacto("donante@test.com", new Email());
        PersonaHumana donante = new PersonaHumana(10L, new ArrayList<>(List.of(emailDonante)), emailDonante, "Juan", "Perez", null, "111", null, null);
        
        Bien bienBase = new Bien("Fideos", 1L, 0.5, 0.5, sub, EstadoBien.NUEVO, LocalDate.now().plusDays(10));
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(100L);
        donacion.setDonante(donante);

        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(donacion, List.of(entidad));
        propuesta.setId(propuestaId);

        when(resultadoRepository.findById(propuestaId)).thenReturn(Optional.of(propuesta));

        matchmakingService.aceptarPropuesta(propuestaId, entidadId);

        assertEquals(EstadoPropuesta.ACEPTADO, propuesta.getEstado());
        assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, donacion.estadoActual());
        assertTrue(necesidad.getDonacionesAsignadas().contains(donacion));

        verify(resultadoRepository, times(1)).save(propuesta);
        verify(donacionRepository, times(1)).save(donacion);
        verify(entidadRepository, times(1)).save(entidad);

        verify(eventManager, times(1)).emitir(any(EventoDonacionAsignadaDonante.class));
        verify(eventManager, times(1)).emitir(any(EventoDonacionAsignadaEntidad.class));
    }

    @Test
    @DisplayName("Debe fallar al aceptar si la entidad no está entre las sugerencias")
    void aceptarPropuestaEntidadInvalida() {
        Long propuestaId = 10L;
        Long entidadIdValida = 1L;
        Long entidadIdInvalida = 2L;

        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123", new ArrayList<>());
        entidad.setId(entidadIdValida);
        
        Categoria cat = new Categoria("Alimentos", false, true);
        Subcategoria sub = new Subcategoria("Fideos", cat);
        Bien bienBase = new Bien("Fideos", 1L, 0.5, 0.5, sub, EstadoBien.NUEVO, LocalDate.now().plusDays(10));
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());

        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(donacion, List.of(entidad));
        propuesta.setId(propuestaId);

        when(resultadoRepository.findById(propuestaId)).thenReturn(Optional.of(propuesta));

        Exception ex = assertThrows(ResourceNotFoundException.class, () -> {
            matchmakingService.aceptarPropuesta(propuestaId, entidadIdInvalida);
        });

        assertTrue(ex.getMessage().contains("La entidad elegida no forma parte de las sugerencias"));
    }

    @Test
    @DisplayName("Debe rechazar la propuesta cambiando su estado a RECHAZADO")
    void rechazarPropuesta() {
        Long propuestaId = 15L;
        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(new Donacion(), new ArrayList<>());
        propuesta.setId(propuestaId);

        when(resultadoRepository.findById(propuestaId)).thenReturn(Optional.of(propuesta));

        matchmakingService.rechazarPropuesta(propuestaId);

        assertEquals(EstadoPropuesta.RECHAZADO, propuesta.getEstado());
        verify(resultadoRepository, times(1)).save(propuesta);
    }
}
