package ar.edu.utn.frba.ddsi.common.security;

import java.util.Objects;

import ar.edu.utn.frba.ddsi.common.exceptions.AccesoDenegadoException;
import ar.edu.utn.frba.ddsi.common.exceptions.NoAutenticadoException;
import io.javalin.http.Context;

/**
 * Chequeos de "solo lo mio" que no se pueden expresar con los roles de la ruta: un donante solo ve sus
 * donaciones, una entidad solo sus necesidades. El administrador y los servicios (SISTEMA) pasan siempre.
 */
public final class Autorizacion {

    static final String ATRIBUTO_USUARIO = "usuarioAutenticado";

    private Autorizacion() {
    }

    /** Usuario del request. Solo existe en rutas que declaran roles. */
    public static UsuarioAutenticado usuario(Context ctx) {
        UsuarioAutenticado usuario = ctx.attribute(ATRIBUTO_USUARIO);
        if (usuario == null) {
            throw new NoAutenticadoException("Se requiere iniciar sesion");
        }
        return usuario;
    }

    public static boolean esPrivilegiado(Context ctx) {
        Rol rol = usuario(ctx).rol();
        return rol == Rol.ADMINISTRADOR || rol == Rol.SISTEMA;
    }

    /** true si el usuario tiene ese rol y su perfil es el indicado. */
    public static boolean esPropietario(Context ctx, Rol rol, Long perfilId) {
        UsuarioAutenticado usuario = usuario(ctx);
        return usuario.rol() == rol && perfilId != null && Objects.equals(usuario.perfilId(), perfilId);
    }

    public static void exigirPropietario(Context ctx, Rol rol, Long perfilId) {
        exigir(esPrivilegiado(ctx) || esPropietario(ctx, rol, perfilId));
    }

    public static void exigir(boolean permitido) {
        if (!permitido) {
            throw new AccesoDenegadoException("No tiene permiso sobre este recurso");
        }
    }
}
