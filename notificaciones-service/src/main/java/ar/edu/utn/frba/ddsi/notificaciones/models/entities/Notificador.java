package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;

@Component
public class Notificador {
    private final Map<TipoContacto, EstrategiaNotificacion> estrategiasContactos;
    private final NotificacionRepository notificacionRepository;

    public Notificador(List<EstrategiaNotificacion> estrategias, NotificacionRepository notificacionRepository) {
        this.estrategiasContactos = estrategias.stream()
                .collect(Collectors.toMap(EstrategiaNotificacion::getTipoContacto, Function.identity()));
        this.notificacionRepository = notificacionRepository;
    }

    public void enviarNotificacion(NotificacionRequest notificacionRequest) {
        if (notificacionRequest == null) {
            throw new BusinessException("La notificación no puede ser nula");
        }
        if (notificacionRequest.getTipoContacto() == null) {
            throw new BusinessException("El medio de contacto no puede ser nulo");
        }
        if (notificacionRequest.getMensaje() == null || notificacionRequest.getMensaje().isBlank()) {
            throw new BusinessException("El mensaje no puede ser nulo ni estar vacío");
        }
        if (notificacionRequest.getValor() == null || notificacionRequest.getValor().isBlank()) {
            throw new BusinessException("El valor del medio de contacto no puede ser nulo ni estar vacío");
        }
        MedioContacto contacto = new MedioContacto(notificacionRequest.getValor(), notificacionRequest.getTipoContacto());
        Notificacion notificacion = new Notificacion(notificacionRequest.getMensaje(), contacto);
        notificacion.setFechaDeEnvio(LocalDateTime.now());
        notificar(notificacion);
        notificacion.setCompletada(true);

        notificacionRepository.save(notificacion);
    }

    public void notificar(Notificacion notificacion) {
        EstrategiaNotificacion estrategia = estrategiasContactos.get(notificacion.getContacto().getTipoContacto());
        if (estrategia == null) {
            throw new BusinessException("No hay estrategia de notificación para el tipo de contacto: " + notificacion.getContacto().getTipoContacto());
        }
        estrategia.notificar(notificacion.getContacto().getValor(), notificacion.getMensaje());
    }
}

