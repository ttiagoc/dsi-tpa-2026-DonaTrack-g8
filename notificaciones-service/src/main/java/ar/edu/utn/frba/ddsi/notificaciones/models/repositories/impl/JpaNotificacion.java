package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
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

  @Override
  public PaginaResponse<Notificacion> findByDestinatarioPaginado(Long destinatarioId, String tipoDestinatario, int page, int size) {
    int paginaSegura = Math.max(1, page);
    int tamanoSeguro = Math.max(1, size);

    Long total = createQuery(
        "select count(n) from Notificacion n where n.destinatarioId = :destinatarioId and " +
            "n.tipoDestinatario = :tipoDestinatario",
        Long.class
    ).setParameter("destinatarioId", destinatarioId)
     .setParameter("tipoDestinatario", tipoDestinatario)
     .getSingleResult();

    int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamanoSeguro));
    int offset = (paginaSegura - 1) * tamanoSeguro;

    List<Notificacion> elementos = createQuery(
        "from Notificacion n where n.destinatarioId = :destinatarioId and " +
            "n.tipoDestinatario = :tipoDestinatario order by n.fechaDeEnvio desc",
        Notificacion.class
    ).setParameter("destinatarioId", destinatarioId)
     .setParameter("tipoDestinatario", tipoDestinatario)
     .setFirstResult(offset)
     .setMaxResults(tamanoSeguro)
     .getResultList();

    return new PaginaResponse<>(elementos, paginaSegura, totalPaginas, total, tamanoSeguro);
  }
}
