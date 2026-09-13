package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.impl;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;

@Repository
public class JpaNotificacion extends RepositorioJpa<Notificacion>
    implements NotificacionRepository {

  public JpaNotificacion() {
      super(Notificacion.class);
  }
}
