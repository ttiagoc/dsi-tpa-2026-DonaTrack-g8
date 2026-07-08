package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;

@Component
public class Notificador {
    private Map<TipoContacto, EstrategiaNotificacion> estrategiasContactos;

    public Notificador(List<EstrategiaNotificacion> estrategias) {
        this.estrategiasContactos = estrategias.stream()
                .collect(Collectors.toMap(EstrategiaNotificacion::getTipoContacto, Function.identity()));
    }

    public void notificar(Notificacion notificacion) {
        EstrategiaNotificacion estrategia = estrategiasContactos.get(notificacion.getContacto().getTipoContacto());
        estrategia.notificar(notificacion.getContacto().getValor(), notificacion.getMensaje());
    }
}
