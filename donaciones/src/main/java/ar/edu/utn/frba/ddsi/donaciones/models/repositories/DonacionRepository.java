package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;

public interface DonacionRepository {

    Donacion save(Donacion donacion);

    List<Donacion> saveAll(List<Donacion> donaciones);

    Optional<Donacion> findById(Long id);

    List<Donacion> findAll();

    boolean deleteById(Long id);

    List<Donacion> buscarPorEstado(TipoEstadoDonacion estadoBuscado);
}
