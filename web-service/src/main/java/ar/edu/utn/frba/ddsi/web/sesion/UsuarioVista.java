package ar.edu.utn.frba.ddsi.web.sesion;

import ar.edu.utn.frba.ddsi.web.dto.auth.UsuarioSesion;

/** Lo que las plantillas ven del usuario logueado ({{usuario.username}}, {{#if usuario.esDonante}}). */
public record UsuarioVista(
        Long usuarioId,
        String username,
        String rol,
        Long perfilId,
        String panel,
        boolean esDonante,
        boolean esEntidad,
        boolean esAdmin) {

    public static UsuarioVista de(UsuarioSesion usuario) {
        RolUsuario rol = usuario.rol();
        return new UsuarioVista(usuario.usuarioId(), usuario.username(), rol.name(), usuario.perfilId(),
                rol.panel(), rol == RolUsuario.DONANTE, rol == RolUsuario.ENTIDAD_BENEFICIARIA,
                rol == RolUsuario.ADMINISTRADOR);
    }
}
