package ar.edu.utn.frba.ddsi.common.security;

/**
 * Quien hace el request, armado a partir de los claims del access token.
 * perfilId es el id del Donante o de la EntidadBeneficiaria (null para el administrador).
 */
public record UsuarioAutenticado(Long usuarioId, String username, Rol rol, Long perfilId) {

    public static UsuarioAutenticado sistema() {
        return new UsuarioAutenticado(null, "sistema", Rol.SISTEMA, null);
    }
}
