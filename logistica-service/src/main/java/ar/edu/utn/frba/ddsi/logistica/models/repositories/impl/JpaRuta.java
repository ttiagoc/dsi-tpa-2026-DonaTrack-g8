package ar.edu.utn.frba.ddsi.logistica.models.repositories.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.enums.EstadoRuta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Repository
public class JpaRuta extends RepositorioJpa<Ruta> implements RutaRepository {

    public JpaRuta() {
        super(Ruta.class);
    }

    @Override
    public List<Ruta> buscarRutasActivas() {
        return createQuery("from Ruta where estado = :enTraslado", Ruta.class)
                .setParameter("enTraslado", EstadoRuta.EN_TRASLADO)
                .getResultList();
    }

    @Override
    public List<Ruta> buscarRutasActivasPorCamion(Long idCamion) {
        if (idCamion == null) {
            return List.of();
        }
        return createQuery("from Ruta where camion.id = :id and estado <> :finalizada", Ruta.class)
                .setParameter("id", idCamion)
                .setParameter("finalizada", EstadoRuta.FINALIZADA)
                .getResultList();
    }

    @Override
    public Ruta buscarRutaDelCamion(Long idCamion) {
        if (idCamion == null) {
            throw new IllegalStateException("El camión no tiene ninguna ruta.");
        }
        return createQuery("from Ruta where camion.id = :id", Ruta.class)
                .setParameter("id", idCamion)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El camión no tiene ninguna ruta."));
    }
}
