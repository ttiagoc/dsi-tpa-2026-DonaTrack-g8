package ar.edu.utn.frba.ddsi.web.sesion;

import java.io.Serializable;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import ar.edu.utn.frba.ddsi.web.dto.auth.SesionResponse;
import ar.edu.utn.frba.ddsi.web.dto.auth.TokensResponse;
import ar.edu.utn.frba.ddsi.web.dto.auth.UsuarioSesion;
import io.javalin.http.Context;

/**
 * Lo que el web-service recuerda de un usuario logueado: los tokens de la API y sus datos.
 * Vive en la sesion de Jetty (memoria del servidor); el navegador solo tiene la cookie con el id.
 * Ningun controller toca ctx.sessionAttribute directamente: todo pasa por aca.
 */
public class SesionUsuario implements Serializable {

    public static final String COOKIE = "donatrack_session";
    private static final String ATRIBUTO = "donatrack.sesion";

    private String accessToken;
    private String refreshToken;
    private final UsuarioSesion usuario;

    public SesionUsuario(String accessToken, String refreshToken, UsuarioSesion usuario) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.usuario = usuario;
    }

    public static SesionUsuario de(SesionResponse respuesta) {
        return new SesionUsuario(respuesta.accessToken(), respuesta.refreshToken(), respuesta.usuario());
    }

    /** Sin crear una sesion nueva: un visitante anonimo no recibe cookie. */
    public static Optional<SesionUsuario> actual(Context ctx) {
        HttpSession sesion = ctx.req().getSession(false);
        if (sesion == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((SesionUsuario) sesion.getAttribute(ATRIBUTO));
    }

    /** Cambia el id de la sesion al loguearse, para que no sirva un id que alguien haya fijado antes. */
    public static void iniciar(Context ctx, SesionUsuario sesionUsuario) {
        if (ctx.req().getSession(false) != null) {
            ctx.req().changeSessionId();
        }
        ctx.req().getSession(true).setAttribute(ATRIBUTO, sesionUsuario);
    }

    public static void cerrar(Context ctx) {
        HttpSession sesion = ctx.req().getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        ctx.removeCookie(COOKIE, "/");
    }

    public synchronized void actualizarTokens(TokensResponse tokens) {
        this.accessToken = tokens.accessToken();
        this.refreshToken = tokens.refreshToken();
    }

    public synchronized String getAccessToken() {
        return accessToken;
    }

    public synchronized String getRefreshToken() {
        return refreshToken;
    }

    public UsuarioSesion getUsuario() {
        return usuario;
    }
}
