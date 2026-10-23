package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios.Usuario;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaUsuario;

@DisplayName("Persistencia de Usuario")
class UsuarioRepositoryJpaTest extends PersistenciaTest {

    private final JpaUsuario usuarios = new JpaUsuario();

    @Test
    @DisplayName("Guarda el usuario y lo encuentra por username sin distinguir mayusculas")
    void buscaPorUsername() {
        Long id = usuarios.save(new Usuario("  Ana.Perez ", "hash", Rol.DONANTE, 42L)).getId();
        nuevoRequest();

        Usuario leido = usuarios.buscarPorUsername("ANA.PEREZ").orElseThrow();

        assertEquals(id, leido.getId());
        assertEquals("ana.perez", leido.getUsername());
        assertEquals(Rol.DONANTE, leido.getRol());
        assertEquals(42L, leido.getPerfilId());
        assertTrue(leido.isActivo());
        assertTrue(usuarios.existeUsername("ana.perez"));
        assertFalse(usuarios.existeUsername("otro"));
    }

    @Test
    @DisplayName("La base no admite dos usuarios con el mismo username")
    void usernameUnico() {
        usuarios.save(new Usuario("admin", "hash", Rol.ADMINISTRADOR, null));
        nuevoRequest();

        assertThrows(RuntimeException.class,
                () -> usuarios.save(new Usuario("ADMIN", "otro-hash", Rol.ADMINISTRADOR, null)));
    }
}
