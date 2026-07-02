package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;

public interface EntidadBeneficiariaRepository {

    EntidadBeneficiaria save(EntidadBeneficiaria entidad);

    Optional<EntidadBeneficiaria> findById(Long id);

    List<EntidadBeneficiaria> findAll();

    boolean deleteById(Long id);

    boolean existsById(Long id);
}
