package ar.edu.utn.frba.ddsi.web.middlewares;

import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.exceptions.AccesoDenegadoException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionRequeridaException;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.security.RouteRole;

/**
 * Mismo patron que el backend: cada ruta declara sus roles en el Router (app.get(ruta, handler, RolUsuario.X))
 * y aca se validan. Ruta sin roles = publica. Sin sesion -> login; rol equivocado -> pagina 403.
 * Esto solo decide que paginas se muestran: la API vuelve a validar cada llamada con el token.
 */
@Component
public class AutorizacionMiddleware {

    public void registrar(Javalin app) {
        app.beforeMatched(this::autorizar);
    }

    private void autorizar(Context ctx) {
        Set<RouteRole> roles = ctx.routeRoles();
        if (roles.isEmpty()) {
            return;
        }
        SesionUsuario sesion = SesionUsuario.actual(ctx).orElseThrow(SesionRequeridaException::new);
        if (!roles.contains(sesion.getUsuario().rol())) {
            throw new AccesoDenegadoException("Esta sección es para otro tipo de usuario.");
        }
    }
}
