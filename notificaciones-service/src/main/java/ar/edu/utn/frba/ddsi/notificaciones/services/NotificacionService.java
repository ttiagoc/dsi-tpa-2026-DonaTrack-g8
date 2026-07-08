package ar.edu.utn.frba.ddsi.notificaciones.services;

import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;

public interface NotificacionService {

    void enviarNotificacion(NotificacionRequest notificacion);
}
