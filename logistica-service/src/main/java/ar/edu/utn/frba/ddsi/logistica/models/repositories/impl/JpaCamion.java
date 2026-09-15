package ar.edu.utn.frba.ddsi.logistica.models.repositories.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@Repository
public class JpaCamion extends RepositorioJpa<Camion> implements CamionRepository {

    public JpaCamion() {
        super(Camion.class);
    }

    @Override
    public Optional<Camion> findByPatente(String patente) {
        if (patente == null) {
            return Optional.empty();
        }
        return createQuery("from Camion where lower(patente) = lower(:patente)", Camion.class)
                .setParameter("patente", patente.trim())
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Camion> findAllDisponibles() {
        return createQuery("select c from Camion c where not exists ("
                + "select r from Ruta r where r.camion = c and r.estado <> :finalizada)", Camion.class)
                .setParameter("finalizada", EstadoRuta.FINALIZADA)
                .getResultList();
    }
}
