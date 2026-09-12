package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Repository
public class JpaDonante extends RepositorioJpa<Donante> implements DonanteRepository {

    public JpaDonante() {
        super(Donante.class);
    }

    @Override
    public Optional<Donante> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return createQuery("select d from Donante d join d.contactos c "
                + "where c.tipoContacto = :tipo and lower(c.valor) = lower(:email)", Donante.class)
                .setParameter("tipo", TipoContacto.EMAIL)
                .setParameter("email", email.trim())
                .getResultStream()
                .findFirst();
    }
}
