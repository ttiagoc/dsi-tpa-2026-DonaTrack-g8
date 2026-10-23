package ar.edu.utn.frba.ddsi.donaciones.models.entities.usuarios;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.common.security.UsuarioAutenticado;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credenciales para entrar a la plataforma. El perfil (Donante o EntidadBeneficiaria) se referencia
 * por id: el usuario solo resuelve el acceso, los datos de la persona siguen en su perfil.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    // id del Donante o de la EntidadBeneficiaria segun el rol; null para el administrador
    @Column(name = "perfil_id")
    private Long perfilId;

    private boolean activo = true;

    public Usuario(String username, String passwordHash, Rol rol, Long perfilId) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("El nombre de usuario no puede estar vacio");
        }
        if (rol == null || rol == Rol.SISTEMA) {
            throw new BusinessException("Un usuario debe ser donante, entidad beneficiaria o administrador");
        }
        if (rol != Rol.ADMINISTRADOR && perfilId == null) {
            throw new BusinessException("Un usuario " + rol + " debe estar asociado a su perfil");
        }
        this.username = normalizar(username);
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.perfilId = perfilId;
    }

    public static String normalizar(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    public UsuarioAutenticado comoAutenticado() {
        return new UsuarioAutenticado(id, username, rol, perfilId);
    }
}
