package ar.edu.utn.frba.ddsi.logistica.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;

public interface CamionRepository {

    Camion save(Camion camion);

    Optional<Camion> findById(Long id);

    Optional<Camion> findByPatente(String patente);

    List<Camion> findAll();

    PaginaResponse<Camion> findAllPaginado(int page, int size);

    List<Camion> findAllDisponibles();

    boolean deleteById(Long id);
}
