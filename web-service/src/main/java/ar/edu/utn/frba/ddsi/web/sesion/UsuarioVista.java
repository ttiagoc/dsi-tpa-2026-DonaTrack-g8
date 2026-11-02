package ar.edu.utn.frba.ddsi.web.sesion;

import ar.edu.utn.frba.ddsi.web.dto.auth.UsuarioSesion;

/** Lo que las plantillas ven del usuario logueado ({{usuario.username}}, {{#if usuario.esDonante}}). */
public record UsuarioVista(
        Long usuarioId,
        String username,
        String rol,
        Long perfilId,
        String panel,
        String rolEtiqueta,
        String iniciales,
        boolean esDonante,
        boolean esEntidad,
        boolean esAdmin) {

    public static UsuarioVista de(UsuarioSesion usuario) {
        RolUsuario rol = usuario.rol();
        return new UsuarioVista(usuario.usuarioId(), usuario.username(), rol.name(), usuario.perfilId(),
                rol.panel(), rol.etiqueta(), iniciales(usuario.username()),
                rol == RolUsuario.DONANTE, rol == RolUsuario.ENTIDAD_BENEFICIARIA,
                rol == RolUsuario.ADMINISTRADOR);
    }

    /** "comedor.sanmartin" -> "CS"; "ana" -> "A". Va en el avatar de la barra superior. */
    static String iniciales(String username) {
        StringBuilder iniciales = new StringBuilder();
        for (String parte : username.split("[^\\p{L}\\p{N}]+")) {
            if (!parte.isEmpty() && iniciales.length() < 2) {
                iniciales.append(parte.charAt(0));
            }
        }
        return iniciales.isEmpty() ? "?" : iniciales.toString().toUpperCase();
    }
}
