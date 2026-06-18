package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.services.NotificacionService;

@DisplayName("NotificacionService Tests")
class NotificacionServiceTest {

    private NotificacionService notificacionService;

    @BeforeEach
    void setUp() {
        notificacionService = new NotificacionService();
        notificacionService.limpiarHistorial();
    }

    @Test
    @DisplayName("Debería enviar notificación y guardarla en el historial como completada")
    void enviarNotificacionExitosamente() {
        // Dado
        MedioContacto email = new Email();
        ((Email) email).setValor("test@utn.edu.ar");
        String mensaje = "Mensaje de prueba de notificación";

        // Cuando
        Notificacion notificacion = notificacionService.enviarNotificacion(email, mensaje);

        // Entonces
        assertNotNull(notificacion);
        assertEquals(mensaje, notificacion.getMensaje());
        assertSame(email, notificacion.getContacto());
        assertTrue(notificacion.getCompletada(), "La notificación debe quedar marcada como completada");
        assertNotNull(notificacion.getFecha(), "Debe haber registrado la fecha de envío");

        // Validar historial
        List<Notificacion> historial = notificacionService.obtenerHistorial();
        assertEquals(1, historial.size(), "El historial debe contener 1 notificación");
        assertSame(notificacion, historial.getFirst(), "La notificación guardada debe ser la misma");
    }

    @Test
    @DisplayName("Debería lanzar excepción si el medio de contacto es nulo")
    void enviarNotificacionContactoNulo() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            notificacionService.enviarNotificacion(null, "Test mensaje");
        });

        assertEquals("El medio de contacto no puede ser nulo", exception.getMessage());
        assertTrue(notificacionService.obtenerHistorial().isEmpty(), "El historial no debe haber sumado nada");
    }
}
