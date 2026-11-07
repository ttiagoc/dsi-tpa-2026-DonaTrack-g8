package ar.edu.utn.frba.ddsi.notificaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

public interface NotificacionRepository {

    Notificacion save(Notificacion notificacion);

    Optional<Notificacion> findById(Long id);

    List<Notificacion> findAll();

    PaginaResponse<Notificacion> findAllPaginado(int page, int size);

    boolean deleteById(Long id);

    List<Notificacion> findByDestinatario(Long destinatarioId, String tipoDestinatario);

    PaginaResponse<Notificacion> findByDestinatarioPaginado(Long destinatarioId, String tipoDestinatario, int page, int size);
}
