package ar.edu.utn.frba.ddsi.notificaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

public interface NotificacionRepository {

    Notificacion save(Notificacion notificacion);

    Optional<Notificacion> findById(Long id);

    List<Notificacion> findAll();

    boolean deleteById(Long id);
}
