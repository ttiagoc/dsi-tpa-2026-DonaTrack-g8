package ar.edu.utn.frba.ddsi.logistica.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;

public interface CamionRepository {

    Camion save(Camion camion);

    Optional<Camion> findById(Long id);

    Optional<Camion> findByPatente(String patente);

    List<Camion> findAll();

    List<Camion> findAllDisponibles();

    boolean deleteById(Long id);
}
