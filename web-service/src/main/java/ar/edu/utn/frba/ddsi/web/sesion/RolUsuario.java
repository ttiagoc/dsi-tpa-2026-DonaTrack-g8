package ar.edu.utn.frba.ddsi.web.sesion;

import io.javalin.security.RouteRole;

/**
 * Mismos nombres que el Rol del backend (llega como string en el JSON del login).
 * Se repite a proposito: el web-service no depende de common-lib.
 */
public enum RolUsuario implements RouteRole {
    DONANTE("/donante"),
    ENTIDAD_BENEFICIARIA("/entidad"),
    ADMINISTRADOR("/admin"),
    SISTEMA("/");

    private final String panel;

    RolUsuario(String panel) {
        this.panel = panel;
    }

    /** Pagina a la que se entra despues del login. */
    public String panel() {
        return panel;
    }
}
