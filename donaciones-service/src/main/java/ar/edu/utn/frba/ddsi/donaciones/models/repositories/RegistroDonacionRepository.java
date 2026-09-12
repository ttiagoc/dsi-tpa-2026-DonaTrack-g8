package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;

public interface RegistroDonacionRepository {

    RegistroDonacion save(RegistroDonacion registroDonacion);

    Optional<RegistroDonacion> findById(Long id);

    List<RegistroDonacion> findAll();
}
