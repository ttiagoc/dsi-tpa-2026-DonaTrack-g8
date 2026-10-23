package ar.edu.utn.frba.ddsi.web.dto.auth;

import java.io.Serializable;

import ar.edu.utn.frba.ddsi.web.sesion.RolUsuario;

public record UsuarioSesion(Long usuarioId, String username, RolUsuario rol, Long perfilId) implements Serializable {
}
