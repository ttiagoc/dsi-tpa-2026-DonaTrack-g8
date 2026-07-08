package ar.edu.utn.frba.ddsi.notificaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificador;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;
import ar.edu.utn.frba.ddsi.notificaciones.services.impl.NotificacionServiceImpl;

@DisplayName("NotificacionService Tests")
class NotificacionServiceTest {

    private NotificacionService notificacionService;
    private Notificador notificador;
    private NotificacionRepository notificacionRepository;

    @BeforeEach
    void setUp() {
        notificador = Mockito.mock(Notificador.class);
        notificacionRepository = Mockito.mock(NotificacionRepository.class);
        notificacionService = new NotificacionServiceImpl(notificador, notificacionRepository);
    }

    @Test
    @DisplayName("Debería enviar notificación y guardarla en el historial como completada")
    void enviarNotificacionExitosamente() {
        String valor = "test@utn.edu.ar";
        TipoContacto tipo = TipoContacto.EMAIL;
        String mensaje = "Mensaje de prueba de notificación";

        NotificacionRequest request = new NotificacionRequest(valor, tipo, mensaje);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);

        notificacionService.enviarNotificacion(request);

        Mockito.verify(notificador).notificar(captor.capture());
        Notificacion notificacionEnviada = captor.getValue();

        assertNotNull(notificacionEnviada);
        assertEquals(mensaje, notificacionEnviada.getMensaje());
        assertNotNull(notificacionEnviada.getContacto());
        assertEquals(valor, notificacionEnviada.getContacto().getValor());
        assertEquals(tipo, notificacionEnviada.getContacto().getTipoContacto());
        assertTrue(notificacionEnviada.getCompletada(), "La notificación debe quedar marcada como completada");
        assertNotNull(notificacionEnviada.getFechaDeEnvio(), "Debe haber registrado la fecha de envío");

        Mockito.verify(notificacionRepository).save(notificacionEnviada);
    }

    @Test
    @DisplayName("Debería lanzar excepción si el request es nulo")
    void enviarNotificacionRequestNulo() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificacionService.enviarNotificacion(null);
        });
        assertEquals("La notificación no puede ser nula", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el medio de contacto es nulo")
    void enviarNotificacionContactoNulo() {
        NotificacionRequest request = new NotificacionRequest("test@utn.edu.ar", null, "Test mensaje");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificacionService.enviarNotificacion(request);
        });
        assertEquals("El medio de contacto no puede ser nulo", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el mensaje es nulo")
    void enviarNotificacionMensajeNulo() {
        NotificacionRequest request = new NotificacionRequest("test@utn.edu.ar", TipoContacto.EMAIL, null);
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificacionService.enviarNotificacion(request);
        });
        assertEquals("El mensaje no puede ser nulo ni estar vacío", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el mensaje está vacío")
    void enviarNotificacionMensajeVacio() {
        NotificacionRequest request = new NotificacionRequest("test@utn.edu.ar", TipoContacto.EMAIL, "  ");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificacionService.enviarNotificacion(request);
        });
        assertEquals("El mensaje no puede ser nulo ni estar vacío", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el valor es nulo")
    void enviarNotificacionValorNulo() {
        NotificacionRequest request = new NotificacionRequest(null, TipoContacto.EMAIL, "Test mensaje");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificacionService.enviarNotificacion(request);
        });
        assertEquals("El valor del medio de contacto no puede ser nulo ni estar vacío", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el valor está vacío")
    void enviarNotificacionValorVacio() {
        NotificacionRequest request = new NotificacionRequest("   ", TipoContacto.EMAIL, "Test mensaje");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificacionService.enviarNotificacion(request);
        });
        assertEquals("El valor del medio de contacto no puede ser nulo ni estar vacío", exception.getMessage());
    }
}

