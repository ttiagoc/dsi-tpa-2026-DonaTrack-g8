package ar.edu.utn.frba.ddsi.common.services.Impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.common.models.entities.Notificador;
import ar.edu.utn.frba.ddsi.common.services.NotificacionService;

@Service
public class NotificacionServiceImpl implements NotificacionService {
  private final Notificador notificador;

  public NotificacionServiceImpl(Notificador notificador) {
    this.notificador = notificador;
  }

  public Notificacion enviarNotificacion(Notificacion notificacion) {

    if (notificacion.getContacto() == null) {
      throw new BusinessException("El medio de contacto no puede ser nulo");
    }
    if (notificacion.getMensaje() == null || notificacion.getMensaje().isBlank()) {
      throw new BusinessException("El mensaje no puede ser nulo ni estar vacío");
    }
    notificacion.setFechaDeEnvio(LocalDateTime.now());
    notificador.notificar(notificacion);
    notificacion.setCompletada(true);

    return notificacion;
  }
}
