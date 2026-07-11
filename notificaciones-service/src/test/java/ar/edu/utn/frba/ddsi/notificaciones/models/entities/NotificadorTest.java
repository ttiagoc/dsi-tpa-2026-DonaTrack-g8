package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;

@DisplayName("Notificador Tests")
class NotificadorTest {

    private Notificador notificador;
    private EstrategiaNotificacion estrategiaMock;
    private NotificacionRepository notificacionRepository;

    @BeforeEach
    void setUp() {
        estrategiaMock = Mockito.mock(EstrategiaNotificacion.class);
        Mockito.when(estrategiaMock.getTipoContacto()).thenReturn(TipoContacto.EMAIL);
        
        List<EstrategiaNotificacion> estrategias = new ArrayList<>();
        estrategias.add(estrategiaMock);

        notificacionRepository = Mockito.mock(NotificacionRepository.class);
        notificador = new Notificador(estrategias, notificacionRepository);
    }

    @Test
    @DisplayName("Debería enviar notificación y guardarla en el historial como completada")
    void enviarNotificacionExitosamente() {
        String valor = "test@utn.edu.ar";
        TipoContacto tipo = TipoContacto.EMAIL;
        String mensaje = "Mensaje de prueba de notificación";

        NotificacionRequest request = new NotificacionRequest(valor, tipo, mensaje);

        notificador.enviarNotificacion(request);

        Mockito.verify(estrategiaMock).notificar(valor, mensaje);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        Mockito.verify(notificacionRepository).save(captor.capture());
        Notificacion notificacionGuardada = captor.getValue();

        assertNotNull(notificacionGuardada);
        assertEquals(mensaje, notificacionGuardada.getMensaje());
        assertNotNull(notificacionGuardada.getContacto());
        assertEquals(valor, notificacionGuardada.getContacto().getValor());
        assertEquals(tipo, notificacionGuardada.getContacto().getTipoContacto());
        assertTrue(notificacionGuardada.getCompletada(), "La notificación debe quedar marcada como completada");
        assertNotNull(notificacionGuardada.getFechaDeEnvio(), "Debe haber registrado la fecha de envío");
    }

    @Test
    @DisplayName("Debería lanzar excepción si el request es nulo")
    void enviarNotificacionRequestNulo() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificador.enviarNotificacion(null);
        });
        assertEquals("La notificación no puede ser nula", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el medio de contacto es nulo")
    void enviarNotificacionContactoNulo() {
        NotificacionRequest request = new NotificacionRequest("test@utn.edu.ar", null, "Test mensaje");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificador.enviarNotificacion(request);
        });
        assertEquals("El medio de contacto no puede ser nulo", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el mensaje es nulo")
    void enviarNotificacionMensajeNulo() {
        NotificacionRequest request = new NotificacionRequest("test@utn.edu.ar", TipoContacto.EMAIL, null);
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificador.enviarNotificacion(request);
        });
        assertEquals("El mensaje no puede ser nulo ni estar vacío", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el mensaje está vacío")
    void enviarNotificacionMensajeVacio() {
        NotificacionRequest request = new NotificacionRequest("test@utn.edu.ar", TipoContacto.EMAIL, "  ");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificador.enviarNotificacion(request);
        });
        assertEquals("El mensaje no puede ser nulo ni estar vacío", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el valor es nulo")
    void enviarNotificacionValorNulo() {
        NotificacionRequest request = new NotificacionRequest(null, TipoContacto.EMAIL, "Test mensaje");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificador.enviarNotificacion(request);
        });
        assertEquals("El valor del medio de contacto no puede ser nulo ni estar vacío", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el valor está vacío")
    void enviarNotificacionValorVacio() {
        NotificacionRequest request = new NotificacionRequest("   ", TipoContacto.EMAIL, "Test mensaje");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            notificador.enviarNotificacion(request);
        });
        assertEquals("El valor del medio de contacto no puede ser nulo ni estar vacío", exception.getMessage());
    }
}
