package ar.edu.utn.frba.ddsi.common.security;

import io.javalin.security.RouteRole;

/**
 * Roles con los que se protegen las rutas de Javalin: app.get(ruta, handler, Rol.ADMINISTRADOR).
 * SISTEMA no es un rol de usuario: lo reciben las llamadas entre servicios que traen la clave compartida.
 */
public enum Rol implements RouteRole {
    DONANTE,
    ENTIDAD_BENEFICIARIA,
    ADMINISTRADOR,
    SISTEMA
}
