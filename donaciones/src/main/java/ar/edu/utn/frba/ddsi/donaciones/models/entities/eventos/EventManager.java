package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventManager {
    private Map<Class<? extends Evento>, List<Listener>> listeners;

    public EventManager() {
        this.listeners = new HashMap<>();
    }

    public void registrarListener(Class<? extends Evento> claseEvento, Listener listener) {
        this.listeners.computeIfAbsent(claseEvento, k -> new ArrayList<>()).add(listener);
    }

    public void desregistrarListener(Class<? extends Evento> claseEvento, Listener listener) {
        this.listeners.get(claseEvento).remove(listener);
    }

    public void emitir(Evento evento) {
        if (evento == null)
            return;

        List<Listener> suscriptores = listeners.get(evento.getClass());

        if (suscriptores != null) {
            for (Listener listener : suscriptores) {
                listener.ejecutar(evento);
            }
        }
    }
}
