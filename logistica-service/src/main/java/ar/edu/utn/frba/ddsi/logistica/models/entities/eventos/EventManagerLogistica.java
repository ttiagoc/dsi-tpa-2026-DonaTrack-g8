package ar.edu.utn.frba.ddsi.logistica.models.entities.eventos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EventManagerLogistica {
    private final Map<Class<? extends EventoLogistica>, List<ListenerLogistica<?>>> listeners;

    public EventManagerLogistica() {
        this.listeners = new HashMap<>();
    }

    public <T extends EventoLogistica> void registrarListener(Class<T> claseEvento, ListenerLogistica<T> listener) {
        this.listeners.computeIfAbsent(claseEvento, k -> new ArrayList<>()).add(listener);
    }

    public <T extends EventoLogistica> void desregistrarListener(Class<T> claseEvento, ListenerLogistica<T> listener) {
        List<ListenerLogistica<?>> suscriptores = this.listeners.get(claseEvento);
        if (suscriptores != null) {
            suscriptores.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends EventoLogistica> void emitir(T evento) {
        if (evento == null)
            return;

        List<ListenerLogistica<?>> suscriptores = listeners.get(evento.getClass());

        if (suscriptores != null) {
            for (ListenerLogistica<?> listener : suscriptores) {
                ((ListenerLogistica<T>) listener).ejecutar(evento);
            }
        }
    }
}
