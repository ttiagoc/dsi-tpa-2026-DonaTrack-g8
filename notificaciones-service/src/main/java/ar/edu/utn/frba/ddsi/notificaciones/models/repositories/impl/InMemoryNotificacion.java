package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.impl;

import org.springframework.stereotype.Repository;
import ar.edu.utn.frba.ddsi.common.utils.GeneradorIdSecuencial;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.NotificacionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InMemoryNotificacion implements NotificacionRepository {
    private final List<Notificacion> notificaciones = new ArrayList<>();

    private final GeneradorIdSecuencial generadorId = new GeneradorIdSecuencial();

    @Override
    public Notificacion save(Notificacion notificacion) {
        if (notificacion.getId() == null) {
            notificacion.setId(generadorId.siguiente());
            notificaciones.add(notificacion);
        } else {
            findById(notificacion.getId()).ifPresent(notificaciones::remove);
            notificaciones.add(notificacion);
        }
        return notificacion;
    }

    @Override
    public Optional<Notificacion> findById(Long id) {
        if (id == null)
            return Optional.empty();
        return notificaciones.stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst();
    }

    @Override
    public List<Notificacion> findAll() {
        return new ArrayList<>(notificaciones);
    }

    @Override
    public boolean deleteById(Long id) {
        Optional<Notificacion> notificacion = findById(id);
        if (notificacion.isPresent()) {
            notificaciones.remove(notificacion.get());
            return true;
        }
        return false;
    }
}
