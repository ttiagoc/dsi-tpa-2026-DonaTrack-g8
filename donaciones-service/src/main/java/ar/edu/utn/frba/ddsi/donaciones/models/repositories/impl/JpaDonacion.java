package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonacionRepository;

@Repository
public class JpaDonacion extends RepositorioJpa<Donacion> implements DonacionRepository {

    public JpaDonacion() {
        super(Donacion.class);
    }

    @Override
    public List<Donacion> saveAll(List<Donacion> donaciones) {
        return withTransaction(() -> {
            List<Donacion> guardadas = new ArrayList<>();
            for (Donacion donacion : donaciones) {
                guardadas.add(save(donacion));
            }
            return guardadas;
        });
    }

    // El estado actual es el ultimo cambio del historial
    @Override
    public List<Donacion> buscarPorEstado(TipoEstadoDonacion estadoBuscado) {
        if (estadoBuscado == null) {
            return new ArrayList<>();
        }
        return createQuery("select d from Donacion d join d.historialEstados h "
                + "where h.estado = :estado and h.fecha = "
                + "(select max(h2.fecha) from Donacion d2 join d2.historialEstados h2 where d2 = d)", Donacion.class)
                .setParameter("estado", estadoBuscado)
                .getResultList();
    }
}
