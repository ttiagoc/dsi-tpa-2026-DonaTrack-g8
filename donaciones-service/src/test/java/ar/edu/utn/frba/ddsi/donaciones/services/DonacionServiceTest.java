package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.BienRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RegistroDonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.DonacionServiceImpl;

@DisplayName("Tests del DonacionService")
class DonacionServiceTest {

    private DonacionRepository donacionRepository;
    private DonacionServiceImpl donacionService;

    private Subcategoria fideos;

    @BeforeEach
    void setUp() {
        donacionRepository = mock(DonacionRepository.class);
        SubcategoriaRepository subcategoriaRepository = mock(SubcategoriaRepository.class);
        DonanteRepository donanteRepository = mock(DonanteRepository.class);
        RegistroDonacionRepository registroDonacionRepository = mock(RegistroDonacionRepository.class);

        donacionService = new DonacionServiceImpl(donacionRepository, new SegmentadorDeDonacion(),
                mock(GestorDeEventos.class), mock(EntidadBeneficiariaRepository.class),
                subcategoriaRepository, donanteRepository, registroDonacionRepository);

        // Alimentos no pide estado pero es perecedero; Mobiliario justo al revés.
        fideos = new Subcategoria("Fideos", new Categoria("Alimentos", false, true));
        Subcategoria sillas = new Subcategoria("Sillas", new Categoria("Mobiliario", true, false));
        when(subcategoriaRepository.findByNombre("Fideos")).thenReturn(Optional.of(fideos));
        when(subcategoriaRepository.findByNombre("Sillas")).thenReturn(Optional.of(sillas));

        MedioContacto email = new MedioContacto("ana@mail.com", TipoContacto.EMAIL);
        Donante ana = new PersonaHumana(List.of(email), email,
                "Ana", "Perez", null, "12345678", "F", "Medrano 951");
        when(donanteRepository.findById(anyLong())).thenReturn(Optional.of(ana));

        // Los repositorios devuelven lo que reciben: aca solo interesa la traduccion del request.
        when(registroDonacionRepository.save(any(RegistroDonacion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(donacionRepository.saveAll(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("Registra un bien sin estado si su categoria no lo pide")
    void bienSinEstadoEnCategoriaQueNoLoPide() {
        DonacionRequest request = new DonacionRequest("Colecta del barrio", 1L, List.of(
                new BienRequest("Fideos secos", 100L, 0.5, 0.001, null,
                        LocalDate.of(2027, 1, 1), new SubcategoriaRequest("Fideos"))));

        List<DonacionResponse> creadas = donacionService.crear(request);
        Donacion guardada = donacionGuardada();

        assertEquals(1, creadas.size());
        assertNull(guardada.getEstadoBienes());
        assertNull(guardada.getBienes().get(0).getEstadoBien());
    }

    @Test
    @DisplayName("Traduce el estado del bien cuando viene, sin importar las mayusculas")
    void bienConEstadoExplicito() {
        DonacionRequest request = new DonacionRequest("Mudanza", 1L, List.of(
                new BienRequest("Silla", 6L, 5.0, 0.3, "usado", null, new SubcategoriaRequest("Sillas"))));

        donacionService.crear(request);

        assertEquals(EstadoBien.USADO, donacionGuardada().getBienes().get(0).getEstadoBien());
    }

    @Test
    @DisplayName("Rechaza un estado de bien que no existe")
    void bienConEstadoInvalido() {
        DonacionRequest request = new DonacionRequest("Mudanza", 1L, List.of(
                new BienRequest("Silla", 6L, 5.0, 0.3, "ROTO", null, new SubcategoriaRequest("Sillas"))));

        BusinessException ex = assertThrows(BusinessException.class, () -> donacionService.crear(request));
        assertTrue(ex.getMessage().contains("ROTO"));
    }

    @Test
    @DisplayName("Sigue exigiendo el estado si la categoria lo pide")
    void bienSinEstadoEnCategoriaQueSiLoPide() {
        DonacionRequest request = new DonacionRequest("Mudanza", 1L, List.of(
                new BienRequest("Silla", 6L, 5.0, 0.3, null, null, new SubcategoriaRequest("Sillas"))));

        BusinessException ex = assertThrows(BusinessException.class, () -> donacionService.crear(request));
        assertTrue(ex.getMessage().contains("exige estado"));
    }

    @Test
    @DisplayName("Cambiar de estado sin indicar cual es un error de request, no una falla interna")
    void cambiarEstadoSinEstado() {
        Bien bien = new Bien("Fideos secos", 100L, 0.5, 0.001, fideos, null, LocalDate.of(2027, 1, 1));
        when(donacionRepository.findById(1L)).thenReturn(Optional.of(new Donacion(bien, LocalDateTime.now())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> donacionService.cambiarEstado(1L, new EstadoDonacionRequest(null, "sin motivo")));
        assertTrue(ex.getMessage().contains("no puede ser nulo"));
    }

    @Test
    @DisplayName("No se puede asignar una donacion cambiando su estado: se asigna aceptando una propuesta")
    void cambiarEstadoAAsignadaFalla() {
        Bien bien = new Bien("Fideos secos", 100L, 0.5, 0.001, fideos, null, LocalDate.of(2027, 1, 1));
        Donacion donacion = new Donacion(bien, LocalDateTime.now());
        when(donacionRepository.findById(1L)).thenReturn(Optional.of(donacion));

        assertThrows(BusinessException.class, () -> donacionService.cambiarEstado(1L,
                new EstadoDonacionRequest("ASIGNACION_REALIZADA", "a mano")));
        assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());
    }

    /** La donacion que el service mando a guardar, para inspeccionar como quedo traducida. */
    @SuppressWarnings("unchecked")
    private Donacion donacionGuardada() {
        ArgumentCaptor<List<Donacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(donacionRepository).saveAll(captor.capture());
        return captor.getValue().get(0);
    }
}
