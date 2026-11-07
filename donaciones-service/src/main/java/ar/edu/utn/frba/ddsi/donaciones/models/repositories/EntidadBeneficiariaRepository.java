package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

public interface EntidadBeneficiariaRepository {

    EntidadBeneficiaria save(EntidadBeneficiaria entidad);

    Optional<EntidadBeneficiaria> findById(Long id);

    List<EntidadBeneficiaria> findAll();

    PaginaResponse<EntidadBeneficiaria> findAllPaginado(int page, int size);

    boolean deleteById(Long id);

    boolean existsById(Long id);
}
