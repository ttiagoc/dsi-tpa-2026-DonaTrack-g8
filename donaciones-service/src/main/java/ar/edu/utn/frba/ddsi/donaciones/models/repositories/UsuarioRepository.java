package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    Optional<Usuario> buscarPorUsername(String username);

    boolean existeUsername(String username);

    Optional<Usuario> buscarPorRolYPerfilId(Rol rol, Long perfilId);

    List<Usuario> listarPorRol(Rol rol);
}
