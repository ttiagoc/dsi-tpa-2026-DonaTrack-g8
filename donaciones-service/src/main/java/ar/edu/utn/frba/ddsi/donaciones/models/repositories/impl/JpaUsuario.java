package ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import ar.edu.utn.frba.ddsi.common.security.Rol;
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

    @Override
    public Optional<Usuario> buscarPorRolYPerfilId(Rol rol, Long perfilId) {
        if (rol == null || perfilId == null) {
            return Optional.empty();
        }
        return createQuery("from Usuario where rol = :rol and perfilId = :perfilId", Usuario.class)
                .setParameter("rol", rol)
                .setParameter("perfilId", perfilId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Usuario> listarPorRol(Rol rol) {
        if (rol == null) {
            return List.of();
        }
        return createQuery("from Usuario where rol = :rol order by id", Usuario.class)
                .setParameter("rol", rol)
                .getResultList();
    }
}
