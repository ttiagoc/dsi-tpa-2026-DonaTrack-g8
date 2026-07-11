package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

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
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.TipoNecesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventManagerDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoDonacionAsignada;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.ResultadoMatchmakingRepository;

@DisplayName("Tests de MotorDeMatchmaking")
class MotorDeMatchmakingTest {

    private DonacionRepository donacionRepository;
    private EntidadBeneficiariaRepository entidadRepository;
    private ResultadoMatchmakingRepository resultadoRepository;
    private EventManagerDonaciones eventManager;
    private List<AlgoritmoAsignacion> algoritmos;
    private MotorDeMatchmaking motorDeMatchmaking;

    @BeforeEach
    void setUp() {
        donacionRepository = mock(DonacionRepository.class);
        entidadRepository = mock(EntidadBeneficiariaRepository.class);
        resultadoRepository = mock(ResultadoMatchmakingRepository.class);
        eventManager = mock(EventManagerDonaciones.class);
        algoritmos = new ArrayList<>();

        motorDeMatchmaking = new MotorDeMatchmaking(algoritmos, donacionRepository, entidadRepository,
                resultadoRepository, eventManager);
    }

    @Test
    @DisplayName("Debe ejecutar matchmaking para donaciones en depósito y guardar los resultados")
    void procesarMatchmakingExitoso() {
        AlgoritmoAsignacion alg = mock(AlgoritmoAsignacion.class);
        algoritmos.add(alg);

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

        when(alg.generarRanking(donacion, entidades)).thenReturn(entidades);

        motorDeMatchmaking.procesarMatchmaking();

        verify(resultadoRepository, times(1)).save(any(ResultadoMatchmaking.class));
    }

    @Test
    @DisplayName("Al aceptar propuesta, debe cambiar estado de donación y notificar")
    void aceptarPropuestaExitosamente() {
        Long propuestaId = 10L;
        Long entidadId = 1L;

        MedioContacto emailEntidad = new MedioContacto("entidad@test.com", TipoContacto.EMAIL);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123",
                new ArrayList<>(List.of(emailEntidad)));
        entidad.setId(entidadId);

        Categoria cat = new Categoria("Alimentos", false, true);
        Subcategoria sub = new Subcategoria("Fideos", cat);

        TipoNecesidad mockTipoNecesidad = mock(TipoNecesidad.class);
        Necesidad necesidad = new Necesidad(sub, mockTipoNecesidad, "Necesitamos fideos", 100L);
        entidad.registrarNecesidad(necesidad);

        MedioContacto emailDonante = new MedioContacto("donante@test.com", TipoContacto.EMAIL);
        PersonaHumana donante = new PersonaHumana(10L, new ArrayList<>(List.of(emailDonante)), emailDonante, "Juan",
                "Perez", null, "111", null, null);

        Bien bienBase = new Bien("Fideos", 1L, 0.5, 0.5, sub, EstadoBien.NUEVO, LocalDate.now().plusDays(10));
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(100L);
        donacion.setDonante(donante);

        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(donacion, List.of(entidad));
        propuesta.setId(propuestaId);

        when(resultadoRepository.findById(propuestaId)).thenReturn(Optional.of(propuesta));

        motorDeMatchmaking.aceptarPropuesta(propuestaId, entidadId);

        assertEquals(EstadoPropuesta.ACEPTADO, propuesta.getEstado());
        assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, donacion.estadoActual());
        assertTrue(necesidad.getDonacionesAsignadas().contains(donacion));

        verify(resultadoRepository, times(1)).save(propuesta);
        verify(donacionRepository, times(1)).save(donacion);
        verify(entidadRepository, times(1)).save(entidad);

        verify(eventManager, times(1)).emitir(any(EventoDonacionAsignada.class));
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
            motorDeMatchmaking.aceptarPropuesta(propuestaId, entidadIdInvalida);
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

        motorDeMatchmaking.rechazarPropuesta(propuestaId);

        assertEquals(EstadoPropuesta.RECHAZADO, propuesta.getEstado());
        verify(resultadoRepository, times(1)).save(propuesta);
    }
}
