package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.common.services.NotificacionService;
import org.springframework.stereotype.Component;

@Component
public class NotificacionesAdapter implements Listener {
    
    private final NotificacionService notificacionService;

    public NotificacionesAdapter(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Override
    public void ejecutar(Evento evento) {
        if (evento == null)
            return;

        String mensaje = evento.getMensaje();
        MedioContacto contacto = evento.getContacto();

        if (contacto != null) {
            notificacionService.enviarNotificacion(new Notificacion(mensaje, contacto));
        } else {
            throw new RuntimeException("Error: no se especificó un canal de contacto válido para el evento.");
        }
    }
}
