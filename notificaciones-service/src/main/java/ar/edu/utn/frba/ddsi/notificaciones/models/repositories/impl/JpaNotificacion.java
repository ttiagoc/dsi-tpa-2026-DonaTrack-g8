package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;

@Repository
public class JpaNotificacion extends RepositorioJpa<Notificacion>
    implements NotificacionRepository {

  public JpaNotificacion() {
      super(Notificacion.class);
  }

  @Override
  public List<Notificacion> findByDestinatario(Long destinatarioId, String tipoDestinatario) {
    return createQuery(

        "from Notificacion n where n.destinatarioId = :destinatarioId and " +
            "n.tipoDestinatario = :tipoDestinatario order by n.fechaDeEnvio desc",
        Notificacion.class
    ).setParameter("destinatarioId", destinatarioId)
     .setParameter("tipoDestinatario", tipoDestinatario)
     .getResultList();
  }
}
