package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

public interface NotificacionService {

    Notificacion enviarNotificacion(Notificacion notificacion);
}
