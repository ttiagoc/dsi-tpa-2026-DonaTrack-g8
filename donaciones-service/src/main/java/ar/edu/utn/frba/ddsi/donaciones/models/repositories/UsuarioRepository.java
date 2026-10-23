package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import java.util.Optional;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    Optional<Usuario> buscarPorUsername(String username);

    boolean existeUsername(String username);
}
