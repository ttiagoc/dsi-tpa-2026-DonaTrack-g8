package ar.edu.utn.frba.ddsi.donaciones.services.notifiactions;

import lombok.Data;
import java.util.Map;

@Data
public class EventManager {
    private Map<TipoEvento, Listener> listeners;

    public void emitir(Evento evento) {
    }
}
