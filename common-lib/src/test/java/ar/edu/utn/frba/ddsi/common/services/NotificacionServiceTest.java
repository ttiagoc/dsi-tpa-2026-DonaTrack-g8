package ar.edu.utn.frba.ddsi.common.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificador;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.common.services.Impl.NotificacionServiceImpl;
import org.mockito.Mockito;

@DisplayName("NotificacionService Tests")
class NotificacionServiceTest {

    private NotificacionService notificacionService;
    private Notificador notificador;

    @BeforeEach
    void setUp() {
        notificador = Mockito.mock(Notificador.class);
        notificacionService = new NotificacionServiceImpl(notificador);
    }

    @Test
    @DisplayName("Debería enviar notificación y guardarla en el historial como completada")
    void enviarNotificacionExitosamente() {
        MedioContacto email = new MedioContacto("test@utn.edu.ar", TipoContacto.EMAIL);
        String mensaje = "Mensaje de prueba de notificación";

        Notificacion nuevaNotificacion = new Notificacion(mensaje, email);
        Notificacion notificacion = notificacionService.enviarNotificacion(nuevaNotificacion);

        assertNotNull(notificacion);
        assertEquals(mensaje, notificacion.getMensaje());
        assertSame(email, notificacion.getContacto());
        assertTrue(notificacion.getCompletada(), "La notificación debe quedar marcada como completada");
        assertNotNull(notificacion.getFechaDeEnvio(), "Debe haber registrado la fecha de envío");
        Mockito.verify(notificador).notificar(nuevaNotificacion);
    }

    @Test
    @DisplayName("Debería lanzar excepción si el medio de contacto es nulo")
    void enviarNotificacionContactoNulo() {
        ar.edu.utn.frba.ddsi.common.exceptions.BusinessException exception = assertThrows(
                ar.edu.utn.frba.ddsi.common.exceptions.BusinessException.class, () -> {
                    notificacionService.enviarNotificacion(new Notificacion("Test mensaje", null));
                });

        assertEquals("El medio de contacto no puede ser nulo", exception.getMessage());
    }
}
