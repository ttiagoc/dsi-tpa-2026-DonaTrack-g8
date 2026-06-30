package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.Data;

@Data
public class NotificacionesAdapter implements Listener {
    @Override
    public void ejecutar(Evento evento) {
        if (evento == null)
            return;

        String mensaje = evento.getMensaje();
        MedioContacto contacto = evento.getContacto();

        if (contacto != null) {
            contacto.notificar(mensaje);
        } else {
            throw new RuntimeException("Error: no se especificó un canal de contacto válido para el evento.");
        }
    }
}
