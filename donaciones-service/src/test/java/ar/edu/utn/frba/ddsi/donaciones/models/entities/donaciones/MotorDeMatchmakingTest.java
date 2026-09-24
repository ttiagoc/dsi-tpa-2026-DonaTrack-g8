package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
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
        PersonaHumana donante = new PersonaHumana(new ArrayList<>(List.of(emailDonante)), emailDonante, "Juan",
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
        assertSame(necesidad, donacion.getNecesidad());
        assertSame(entidad, donacion.getEntidadBeneficiariaAsignada());
        assertEquals("Dir 1", donacion.obtenerDireccion());

        verify(resultadoRepository, times(1)).save(propuesta);
        verify(donacionRepository, times(1)).save(donacion);

        verify(eventManager, times(1)).emitir(any(EventoDonacionAsignada.class));
    }

    @Test
    @DisplayName("No se puede aceptar una propuesta que ya fue resuelta")
    void aceptarPropuestaYaResuelta() {
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123", new ArrayList<>());
        entidad.setId(1L);
        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(donacionEnDeposito(), List.of(entidad));
        propuesta.setId(10L);
        propuesta.setEstado(EstadoPropuesta.ACEPTADO);
        when(resultadoRepository.findById(10L)).thenReturn(Optional.of(propuesta));

        Exception ex = assertThrows(BusinessException.class, () -> motorDeMatchmaking.aceptarPropuesta(10L, 1L));

        assertTrue(ex.getMessage().contains("ya fue resuelta"));
        verify(donacionRepository, never()).save(any());
        verify(eventManager, never()).emitir(any());
    }

    @Test
    @DisplayName("No se puede aceptar una propuesta si la donacion ya no esta en deposito")
    void aceptarPropuestaDeDonacionYaAsignada() {
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123", new ArrayList<>());
        entidad.setId(1L);
        Donacion donacion = donacionEnDeposito();
        donacion.asignarA(entidad, null);
        ResultadoMatchmaking propuestaVieja = new ResultadoMatchmaking(donacion, List.of(entidad));
        propuestaVieja.setId(11L);
        when(resultadoRepository.findById(11L)).thenReturn(Optional.of(propuestaVieja));

        assertThrows(BusinessException.class, () -> motorDeMatchmaking.aceptarPropuesta(11L, 1L));

        assertEquals(EstadoPropuesta.PENDIENTE, propuestaVieja.getEstado());
        verify(resultadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede rechazar una propuesta que ya fue aceptada")
    void rechazarPropuestaYaAceptada() {
        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(new Donacion(), new ArrayList<>());
        propuesta.setId(15L);
        propuesta.setEstado(EstadoPropuesta.ACEPTADO);
        when(resultadoRepository.findById(15L)).thenReturn(Optional.of(propuesta));

        assertThrows(BusinessException.class, () -> motorDeMatchmaking.rechazarPropuesta(15L));

        assertEquals(EstadoPropuesta.ACEPTADO, propuesta.getEstado());
    }

    @Test
    @DisplayName("El matchmaking no genera otra propuesta para una donacion que ya tiene una pendiente")
    void procesarMatchmakingNoDuplicaPropuestas() {
        AlgoritmoAsignacion alg = mock(AlgoritmoAsignacion.class);
        algoritmos.add(alg);
        Donacion donacion = donacionEnDeposito();
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Dir 1", "123", new ArrayList<>());
        entidad.setId(1L);

        when(donacionRepository.buscarPorEstado(TipoEstadoDonacion.EN_DEPOSITO)).thenReturn(List.of(donacion));
        when(entidadRepository.findAll()).thenReturn(List.of(entidad));
        when(resultadoRepository.buscarPendientes())
                .thenReturn(List.of(new ResultadoMatchmaking(donacion, List.of(entidad))));

        motorDeMatchmaking.procesarMatchmaking();

        verify(resultadoRepository, never()).save(any());
    }

    private Donacion donacionEnDeposito() {
        Subcategoria sub = new Subcategoria("Fideos", new Categoria("Alimentos", false, true));
        Bien bienBase = new Bien("Fideos", 1L, 0.5, 0.5, sub, EstadoBien.NUEVO, LocalDate.now().plusDays(10));
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(100L);
        return donacion;
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
