package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.UsuarioRepository;

@Repository
public class JpaUsuario extends RepositorioJpa<Usuario> implements UsuarioRepository {

    public JpaUsuario() {
        super(Usuario.class);
    }

    // los usernames se guardan normalizados (ver Usuario.normalizar), asi la busqueda no distingue mayusculas
    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return createQuery("from Usuario where username = :username", Usuario.class)
                .setParameter("username", Usuario.normalizar(username))
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existeUsername(String username) {
        return buscarPorUsername(username).isPresent();
    }
}
