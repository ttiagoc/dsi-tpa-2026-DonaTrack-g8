package ar.edu.utn.frba.ddsi.common.services;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificacion;

public interface NotificacionService {

    Notificacion enviarNotificacion(MedioContacto contacto, String mensaje);
}
