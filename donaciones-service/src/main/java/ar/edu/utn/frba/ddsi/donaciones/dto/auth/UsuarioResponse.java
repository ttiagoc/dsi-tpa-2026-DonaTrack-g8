package ar.edu.utn.frba.ddsi.donaciones.dto.auth;

import ar.edu.utn.frba.ddsi.common.security.Rol;

public record UsuarioResponse(Long usuarioId, String username, Rol rol, Long perfilId) {
}
