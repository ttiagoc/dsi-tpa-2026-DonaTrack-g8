package ar.edu.utn.frba.ddsi.donaciones.models.entities.notificaciones;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEvento;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventManager {
    private Map<TipoEvento, List<Listener>> listeners;

    public EventManager() {
        listeners = new EnumMap<>(TipoEvento.class);
        for (TipoEvento tipo : TipoEvento.values()) {
            listeners.put(tipo, new ArrayList<Listener>());
        }
    }

    public void emitir(Evento evento) {
        if (evento == null)
            return;

        List<Listener> suscriptores = listeners.get(evento.getTipo());
        for (Listener listener : suscriptores) {
            listener.ejecutar(evento);
        }
    }
}
