package ar.edu.utn.frba.ddsi.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.exceptions.AccesoDenegadoException;
import ar.edu.utn.frba.ddsi.common.exceptions.NoAutenticadoException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.security.RouteRole;

/**
 * Protege las rutas que declaran roles (app.get(ruta, handler, Rol.X)); las rutas sin roles son publicas.
 * Acepta dos credenciales:
 * - "Authorization: Bearer <access token>" para los usuarios (lo manda el web-service).
 * - "X-Api-Key: <SERVICE_API_KEY>" para las llamadas entre servicios, que quedan con rol SISTEMA.
 * Si pasa, deja el usuario en el contexto para los controllers (ver Autorizacion).
 */
@Component
public class AutenticacionHandler implements JavalinController {

    public static final String HEADER_API_KEY = "X-Api-Key";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final byte[] serviceApiKey;

    public AutenticacionHandler(JwtUtil jwtUtil, @Value("${seguridad.service-api-key:}") String serviceApiKey) {
        this.jwtUtil = jwtUtil;
        this.serviceApiKey = serviceApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.beforeMatched(this::autenticar);
    }

    private void autenticar(Context ctx) {
        Set<RouteRole> rolesPermitidos = ctx.routeRoles();
        if (rolesPermitidos.isEmpty()) {
            return;
        }

        UsuarioAutenticado usuario = identificar(ctx);
        if (!rolesPermitidos.contains(usuario.rol())) {
            throw new AccesoDenegadoException("El rol " + usuario.rol() + " no puede acceder a este recurso");
        }
        ctx.attribute(Autorizacion.ATRIBUTO_USUARIO, usuario);
    }

    private UsuarioAutenticado identificar(Context ctx) {
        String apiKey = ctx.header(HEADER_API_KEY);
        if (apiKey != null) {
            // comparacion en tiempo constante para no filtrar la clave por tiempos de respuesta
            boolean valida = serviceApiKey.length > 0
                    && MessageDigest.isEqual(serviceApiKey, apiKey.getBytes(StandardCharsets.UTF_8));
            if (!valida) {
                throw new NoAutenticadoException("Clave de servicio invalida");
            }
            return UsuarioAutenticado.sistema();
        }

        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            throw new NoAutenticadoException("Se requiere iniciar sesion");
        }
        return jwtUtil.leerAccessToken(header.substring(PREFIJO_BEARER.length()));
    }
}
