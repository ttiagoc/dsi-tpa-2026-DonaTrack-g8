package ar.edu.utn.frba.ddsi.common.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificacion;

@DisplayName("NotificacionService Tests")
class NotificacionServiceTest {

    private NotificacionService notificacionService;

    @BeforeEach
    void setUp() {
        notificacionService = new NotificacionService();
    }

    @Test
    @DisplayName("Debería enviar notificación y guardarla en el historial como completada")
    void enviarNotificacionExitosamente() {
        MedioContacto email = new Email();
        ((Email) email).setValor("test@utn.edu.ar");
        String mensaje = "Mensaje de prueba de notificación";

        Notificacion notificacion = notificacionService.enviarNotificacion(email, mensaje);

        assertNotNull(notificacion);
        assertEquals(mensaje, notificacion.getMensaje());
        assertSame(email, notificacion.getContacto());
        assertTrue(notificacion.getCompletada(), "La notificación debe quedar marcada como completada");
        assertNotNull(notificacion.getFecha(), "Debe haber registrado la fecha de envío");
    }

    @Test
    @DisplayName("Debería lanzar excepción si el medio de contacto es nulo")
    void enviarNotificacionContactoNulo() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            notificacionService.enviarNotificacion(null, "Test mensaje");
        });

        assertEquals("El medio de contacto no puede ser nulo", exception.getMessage());
    }
}
