package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;

@Repository
public class JpaEntidadBeneficiaria extends RepositorioJpa<EntidadBeneficiaria>
        implements EntidadBeneficiariaRepository {

    public JpaEntidadBeneficiaria() {
        super(EntidadBeneficiaria.class);
    }
}
