package ar.edu.utn.frba.ddsi.logistica.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;

public interface RutaRepository {

    Ruta save(Ruta ruta);

    Optional<Ruta> findById(Long id);

    List<Ruta> findAll();

    PaginaResponse<Ruta> findAllPaginado(int page, int size);

    List<Ruta> buscarRutasActivas();

    boolean deleteById(Long id);

    List<Ruta> buscarRutasActivasPorCamion(Long idCamion);

    Ruta buscarRutaDelCamion(Long idCamion);

    List<Ruta> buscarRutasActivasFiltradas(Long entidadId, List<Long> donacionIds);
}
