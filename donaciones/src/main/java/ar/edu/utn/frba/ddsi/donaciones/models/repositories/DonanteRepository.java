package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;

public interface DonanteRepository {

    Donante save(Donante donante);

    Optional<Donante> findById(Long id);

    List<Donante> findAll();

    boolean deleteById(Long id);

    Optional<Donante> buscarPorEmail(String email);

    void limpiar();

    boolean existsById(Long id);
}
