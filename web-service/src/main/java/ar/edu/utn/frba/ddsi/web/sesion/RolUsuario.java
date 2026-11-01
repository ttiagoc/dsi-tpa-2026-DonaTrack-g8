package ar.edu.utn.frba.ddsi.web.sesion;

import io.javalin.security.RouteRole;

/**
 * Mismos nombres que el Rol del backend (llega como string en el JSON del login).
 * Se repite a proposito: el web-service no depende de common-lib.
 */
public enum RolUsuario implements RouteRole {
    DONANTE("/donante", "Donante", "Panel del donante"),
    ENTIDAD_BENEFICIARIA("/entidad", "Entidad beneficiaria", "Panel de la entidad"),
    ADMINISTRADOR("/admin", "Administrador", "Administración"),
    SISTEMA("/", "Sistema", "DonaTrack");

    private final String panel;
    private final String etiqueta;
    private final String tituloPanel;

    RolUsuario(String panel, String etiqueta, String tituloPanel) {
        this.panel = panel;
        this.etiqueta = etiqueta;
        this.tituloPanel = tituloPanel;
    }

    /** Pagina a la que se entra despues del login. */
    public String panel() {
        return panel;
    }

    /** Como se muestra el rol en el perfil de la barra superior. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Titulo de la barra superior. */
    public String tituloPanel() {
        return tituloPanel;
    }
}
