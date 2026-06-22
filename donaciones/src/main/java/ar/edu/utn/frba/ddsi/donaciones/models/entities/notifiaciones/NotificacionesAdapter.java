package ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones;

import lombok.Data;

@Data
public class NotificacionesAdapter implements Listener {
    @Override
    public void ejecutar(Evento evento) {
    }

    public String armarMensaje(Evento evento) {
        return null;
    }
}
