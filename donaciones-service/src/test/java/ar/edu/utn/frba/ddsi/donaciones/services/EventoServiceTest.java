package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ParadaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventManagerDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoAusenciaPlataforma;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaExitosa;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.EventoEntregaFallida;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.EventoServiceImpl;

@DisplayName("Tests de EventoServiceImpl")
class EventoServiceTest {

    private EventManagerDonaciones eventManager;
    private DonacionRepository donacionRepository;
    private EntidadBeneficiariaRepository entidadRepository;
    private DonanteRepository donanteRepository;
    private EventoServiceImpl eventoService;

    @BeforeEach
    void setUp() {
        eventManager = mock(EventManagerDonaciones.class);
        donacionRepository = mock(DonacionRepository.class);
        entidadRepository = mock(EntidadBeneficiariaRepository.class);
        donanteRepository = mock(DonanteRepository.class);

        eventoService = new EventoServiceImpl(eventManager, donacionRepository, entidadRepository, donanteRepository);
    }

    @Test
    @DisplayName("Debe detectar inactividad y enviar notificación si donó hace más de 20 días")
    void verificarInactividadDonantes() {
        PersonaHumana donanteInactivo = new PersonaHumana();
        donanteInactivo.setId(1L);
        RegistroDonacion regInactivo = new RegistroDonacion();
        regInactivo.setFecha(LocalDateTime.now().minusDays(30)); // 30 días, debe notificar
        donanteInactivo.agregarDonacion(regInactivo);
        donanteInactivo.setContactoPredeterminado(new MedioContacto("inactivo@test.com", TipoContacto.EMAIL));

        PersonaHumana donanteActivo = new PersonaHumana();
        donanteActivo.setId(2L);
        RegistroDonacion regActivo = new RegistroDonacion();
        regActivo.setFecha(LocalDateTime.now().minusDays(5)); // 5 días, NO debe notificar
        donanteActivo.agregarDonacion(regActivo);

        when(donanteRepository.findAll()).thenReturn(List.of(donanteInactivo, donanteActivo));

        eventoService.verificarInactividadDonantes();

        verify(eventManager, times(1)).emitir(any(EventoAusenciaPlataforma.class));
    }

    @Test
    @DisplayName("Al iniciar ruta, cambia estados y emite eventos a donantes y entidades")
    void iniciarRuta() {
        Long entidadId = 1L;
        Long donacionId = 100L;

        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Dir", "123",
                new ArrayList<>(List.of(new MedioContacto("hogar@test.com", TipoContacto.EMAIL))));
        entidad.setId(entidadId);

        PersonaHumana donante = new PersonaHumana();
        donante.setContactoPredeterminado(new MedioContacto("donante@test.com", TipoContacto.EMAIL));

        Categoria cat = new Categoria("Muebles", true, false);
        Subcategoria sub = new Subcategoria("Sillas", cat);
        Bien bienBase = new Bien("Silla", 1L, 1.0, 1.0, sub, EstadoBien.NUEVO, null);
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(donacionId);
        donacion.setDonante(donante);

        when(entidadRepository.findById(entidadId)).thenReturn(Optional.of(entidad));
        when(donacionRepository.findById(donacionId)).thenReturn(Optional.of(donacion));

        ParadaRequest parada = new ParadaRequest(entidadId, List.of(donacionId));
        InicioRutaRequest request = new InicioRutaRequest(10L, List.of(parada));

        eventoService.iniciarRuta(request);

        assertEquals(TipoEstadoDonacion.EN_TRASLADO, donacion.estadoActual());
        verify(donacionRepository, times(1)).save(donacion);
    }

    @Test
    @DisplayName("Debe procesar confirmación exitosa y emitir eventos")
    void confirmarEntregaExitosa() {
        Long entidadId = 1L;
        Long donacionId = 100L;

        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Dir", "123",
                new ArrayList<>(List.of(new MedioContacto("hogar@test.com", TipoContacto.EMAIL))));
        entidad.setId(entidadId);

        PersonaHumana donante = new PersonaHumana();
        donante.setContactoPredeterminado(new MedioContacto("donante@test.com", TipoContacto.EMAIL));

        Categoria cat = new Categoria("Muebles", true, false);
        Subcategoria sub = new Subcategoria("Sillas", cat);
        Bien bienBase = new Bien("Silla", 1L, 1.0, 1.0, sub, EstadoBien.NUEVO, null);
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(donacionId);
        donacion.setDonante(donante);

        when(entidadRepository.findById(entidadId)).thenReturn(Optional.of(entidad));
        when(donacionRepository.findById(donacionId)).thenReturn(Optional.of(donacion));

        ConfirmacionEntregaExitosaRequest request = new ConfirmacionEntregaExitosaRequest(entidadId,
                List.of(donacionId), "AB123CD", LocalDateTime.now());
        eventoService.confirmarEntregaExitosa(request);

        assertEquals(TipoEstadoDonacion.ENTREGADA, donacion.estadoActual());
        verify(donacionRepository, times(1)).save(donacion);

        verify(eventManager, times(1)).emitir(any(EventoEntregaExitosa.class));
    }

    @Test
    @DisplayName("Debe reportar entrega fallida cambiando el estado y notificando")
    void notificarEntregaFallida() {
        Long donacionId = 100L;

        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Dir", "123",
                new ArrayList<>(List.of(new MedioContacto("hogar@test.com", TipoContacto.EMAIL))));
        PersonaHumana donante = new PersonaHumana();
        donante.setContactoPredeterminado(new MedioContacto("donante@test.com", TipoContacto.EMAIL));

        Categoria cat = new Categoria("Muebles", true, false);
        Subcategoria sub = new Subcategoria("Sillas", cat);
        Bien bienBase = new Bien("Silla", 1L, 1.0, 1.0, sub, EstadoBien.NUEVO, null);
        Donacion donacion = new Donacion(bienBase, LocalDateTime.now());
        donacion.setId(donacionId);
        donacion.setDonante(donante);
        donacion.setEntidadBeneficiariaAsignada(entidad);

        when(donacionRepository.findById(donacionId)).thenReturn(Optional.of(donacion));

        eventoService.notificarEntregaFallida(donacionId, "Camión averiado");

        assertEquals(TipoEstadoDonacion.ENTREGA_FALLIDA, donacion.estadoActual());
        assertTrue(donacion.getHistorialEstados().getLast().getJustificacion().contains("Camión averiado"));

        verify(donacionRepository, times(1)).save(donacion);
        verify(eventManager, times(1)).emitir(any(EventoEntregaFallida.class));
    }
}
