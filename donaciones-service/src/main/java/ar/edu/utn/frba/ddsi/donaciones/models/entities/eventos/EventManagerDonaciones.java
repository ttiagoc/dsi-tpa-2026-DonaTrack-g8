package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
public class EventManagerDonaciones {
    private Map<Class<? extends EventoDonaciones>, List<ListenerDonaciones<?>>> listeners;

    public EventManagerDonaciones() {
        this.listeners = new HashMap<>();
    }

    public <T extends EventoDonaciones> void registrarListener(Class<T> claseEvento, ListenerDonaciones<T> listener) {
        this.listeners.computeIfAbsent(claseEvento, k -> new ArrayList<>()).add(listener);
    }

    public <T extends EventoDonaciones> void desregistrarListener(Class<T> claseEvento, ListenerDonaciones<T> listener) {
        List<ListenerDonaciones<?>> suscriptores = this.listeners.get(claseEvento);
        if (suscriptores != null) {
            suscriptores.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends EventoDonaciones> void emitir(T evento) {
        if (evento == null)
            return;

        List<ListenerDonaciones<?>> suscriptores = listeners.get(evento.getClass());

        if (suscriptores != null) {
            for (ListenerDonaciones<?> listener : suscriptores) {
                ((ListenerDonaciones<T>) listener).ejecutar(evento);
            }
        }
    }
}
