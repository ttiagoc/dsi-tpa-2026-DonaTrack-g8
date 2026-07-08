package ar.edu.utn.frba.ddsi.notificaciones.services.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificador;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificacionService;

@Service
public class NotificacionServiceImpl implements NotificacionService {
  private final Notificador notificador;
  private final NotificacionRepository notificacionRepository;

  public NotificacionServiceImpl(Notificador notificador, NotificacionRepository notificacionRepository) {
    this.notificador = notificador;
    this.notificacionRepository = notificacionRepository;
  }

  @Override
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
    notificador.notificar(notificacion);
    notificacion.setCompletada(true);

    return notificacionRepository.save(notificacion);
  }
}

