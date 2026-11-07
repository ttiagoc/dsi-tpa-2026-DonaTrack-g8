package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;

public interface DonacionRepository {

    Donacion save(Donacion donacion);

    List<Donacion> saveAll(List<Donacion> donaciones);

    Optional<Donacion> findById(Long id);

    List<Donacion> findAll();

    boolean deleteById(Long id);

    List<Donacion> buscarPorEstado(TipoEstadoDonacion estadoBuscado);

    List<Donacion> buscarPorEntidad(Long entidadId);
    List<Donacion> buscar(Long donanteId, TipoEstadoDonacion estado, Long categoriaId,
                          List<Long> subcategoriaIds, Long entidadId);

    PaginaResponse<Donacion> buscarPaginado(Long donanteId, TipoEstadoDonacion estado, Long categoriaId,
                                            List<Long> subcategoriaIds, Long entidadId, int page, int size);
}
