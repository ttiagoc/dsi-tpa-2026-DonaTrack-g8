package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RegistroDonacionRepository;

@Repository
public class JpaRegistroDonacion extends RepositorioJpa<RegistroDonacion> implements RegistroDonacionRepository {

    public JpaRegistroDonacion() {
        super(RegistroDonacion.class);
    }
}
