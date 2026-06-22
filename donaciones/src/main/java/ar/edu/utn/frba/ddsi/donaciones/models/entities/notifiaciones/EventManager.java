package ar.edu.utn.frba.ddsi.donaciones.models.entities.notifiaciones;

import lombok.Data;
import java.util.Map;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEvento;

@Data
public class EventManager {
    private Map<TipoEvento, Listener> listeners;

    public void emitir(Evento evento) {
    }
}
