package ar.edu.utn.frba.ddsi.common.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificacion;

@Service
public class NotificacionServiceImpl implements NotificacionService {
  public Notificacion enviarNotificacion(MedioContacto contacto, String mensaje) {
    if (contacto == null) {
      throw new IllegalArgumentException("El medio de contacto no puede ser nulo");
    }

    Notificacion notificacion = new Notificacion(LocalDateTime.now(), mensaje, contacto, false);
    contacto.notificar(mensaje);
    notificacion.setCompletada(true);

    return notificacion;
  }
}
