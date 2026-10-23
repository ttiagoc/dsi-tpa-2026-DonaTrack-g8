package ar.edu.utn.frba.ddsi.web.server;

import java.util.Map;

import jakarta.servlet.SessionTrackingMode;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.session.SessionHandler;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.config.WebConfig;
import ar.edu.utn.frba.ddsi.web.controllers.Modelo;
import ar.edu.utn.frba.ddsi.web.exceptions.AccesoDenegadoException;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiNoDisponibleException;
import ar.edu.utn.frba.ddsi.web.exceptions.NoEncontradoException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionRequeridaException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionVencidaException;
import ar.edu.utn.frba.ddsi.web.server.templates.JavalinHandlebars;
import ar.edu.utn.frba.ddsi.web.server.templates.JavalinRenderer;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

/**
 * Configuracion del servidor Javalin: plantillas, archivos estaticos, sesion y paginas de error.
 * Las rutas estan en Router.
 */
@Component
public class Server {

    private static final String PAGINA_DE_ERROR = "donatrack.paginaDeError";

    private final Router router;
    private final WebConfig config;
    private Javalin app;

    public Server(Router router, WebConfig config) {
        this.router = router;
        this.config = config;
    }

    public Javalin crear() {
        Javalin javalin = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
            javalinConfig.fileRenderer(new JavalinRenderer().register("hbs", new JavalinHandlebars()));
            javalinConfig.staticFiles.add("/public", Location.CLASSPATH);
            javalinConfig.jetty.modifyServletContextHandler(handler -> handler.setSessionHandler(sesiones()));
        });
        registrarErrores(javalin);
        router.registrar(javalin);
        return javalin;
    }

    public void iniciar(int puerto) {
        this.app = crear().start(puerto);
    }

    public void detener() {
        if (app != null) {
            app.stop();
        }
    }

    /**
     * Sesion en memoria del servidor: la cookie solo lleva un id aleatorio.
     * HttpOnly: el JavaScript de la pagina no puede leerla. SameSite=Lax: el navegador no la manda en POSTs
     * que vienen de otros sitios (defensa contra CSRF). Secure en la nube: solo viaja por HTTPS.
     */
    private SessionHandler sesiones() {
        SessionHandler sesiones = new SessionHandler();
        sesiones.setSessionCookie(SesionUsuario.COOKIE);
        sesiones.setHttpOnly(true);
        sesiones.setSameSite(HttpCookie.SameSite.LAX);
        sesiones.getSessionCookieConfig().setSecure(config.getSesion().isCookieSegura());
        sesiones.setMaxInactiveInterval(config.getSesion().getMinutosInactividad() * 60);
        // sin ;jsessionid=... en las URLs
        sesiones.setSessionTrackingModes(java.util.Set.of(SessionTrackingMode.COOKIE));
        return sesiones;
    }

    private void registrarErrores(Javalin javalin) {
        javalin.exception(SesionRequeridaException.class, (e, ctx) -> ctx.redirect("/login"));
        javalin.exception(SesionVencidaException.class, (e, ctx) -> ctx.redirect("/login?vencida"));
        javalin.exception(AccesoDenegadoException.class, (e, ctx) -> paginaDeError(ctx, 403, e.getMessage()));
        javalin.exception(NoEncontradoException.class, (e, ctx) -> paginaDeError(ctx, 404, e.getMessage()));
        javalin.exception(ApiNoDisponibleException.class, (e, ctx) -> {
            System.err.println("API no disponible: " + e.getMessage());
            paginaDeError(ctx, 503, null);
        });
        javalin.exception(ApiException.class, (e, ctx) -> {
            if (e.getStatus() == 404) {
                paginaDeError(ctx, 404, e.getMessage());
                return;
            }
            System.err.println("Error de la API (" + e.getStatus() + "): " + e.getMessage());
            paginaDeError(ctx, 500, null);
        });
        javalin.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            paginaDeError(ctx, 500, null);
        });
        // rutas que no existen (reemplaza el texto plano "Endpoint ... not found" de Javalin)
        // (Javalin corre este handler para cualquier 404, tambien despues de un exception handler: no se pisa
        // la pagina que ya se armo con su mensaje)
        javalin.error(404, ctx -> {
            if (ctx.attribute(PAGINA_DE_ERROR) == null) {
                paginaDeError(ctx, 404, null);
            }
        });
    }

    private void paginaDeError(Context ctx, int status, String mensaje) {
        ctx.status(status);
        ctx.attribute(PAGINA_DE_ERROR, true);
        try {
            Map<String, Object> modelo = Modelo.base(ctx);
            if (mensaje != null) {
                modelo.put("mensaje", mensaje);
            }
            ctx.render("errores/" + status + ".html.hbs", modelo);
        } catch (RuntimeException e) {
            // si falla la propia plantilla de error, al menos se responde algo
            e.printStackTrace();
            ctx.result("Ocurrió un error (" + status + ")");
        }
    }
}
