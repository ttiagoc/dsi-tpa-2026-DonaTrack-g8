package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.http.Context;

/** Login y logout. La validacion real de las credenciales la hace /api/auth/login. */
@Component
public class SesionController {

    private static final String VISTA_LOGIN = "auth/login.html.hbs";

    private final AuthApiClient auth;

    public SesionController(AuthApiClient auth) {
        this.auth = auth;
    }

    public void mostrarLogin(Context ctx) {
        Optional<SesionUsuario> sesion = SesionUsuario.actual(ctx);
        if (sesion.isPresent()) {
            ctx.redirect(sesion.get().getUsuario().rol().panel());
            return;
        }
        Map<String, Object> modelo = Modelo.base(ctx);
        if (ctx.queryParam("vencida") != null) {
            Modelo.conAlerta(modelo, "info", "Tu sesión venció. Volvé a ingresar.");
        }
        ctx.render(VISTA_LOGIN, new Formulario(ctx).enModelo(modelo, null));
    }

    public void login(Context ctx) {
        Formulario form = new Formulario(ctx);
        String username = form.requerido("username", "El usuario");
        String password = form.requerido("password", "La contraseña");
        if (form.tieneErrores()) {
            mostrarError(ctx, form, null);
            return;
        }

        try {
            SesionUsuario sesion = auth.login(username, password);
            SesionUsuario.iniciar(ctx, sesion);
            Alertas.exito(ctx, "¡Hola, " + sesion.getUsuario().username() + "!");
            ctx.redirect(sesion.getUsuario().rol().panel());
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            mostrarError(ctx, form, e.getStatus() == 401 ? "Usuario o contraseña incorrectos" : e.getMessage());
        }
    }

    public void logout(Context ctx) {
        SesionUsuario.cerrar(ctx);
        ctx.redirect("/?logout");
    }

    private void mostrarError(Context ctx, Formulario form, String error) {
        ctx.status(400).render(VISTA_LOGIN, form.enModelo(Modelo.base(ctx), error));
    }
}
