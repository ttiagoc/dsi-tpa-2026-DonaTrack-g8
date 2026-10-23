package ar.edu.utn.frba.ddsi.web.server;

import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.DONANTE;
import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.ENTIDAD_BENEFICIARIA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.controllers.HomeController;
import ar.edu.utn.frba.ddsi.web.controllers.PanelController;
import ar.edu.utn.frba.ddsi.web.controllers.RegistroController;
import ar.edu.utn.frba.ddsi.web.controllers.SesionController;
import ar.edu.utn.frba.ddsi.web.middlewares.AutorizacionMiddleware;
import io.javalin.Javalin;

/**
 * Unico lugar donde se declaran las rutas (apunte de MVC, seccion 8.2: referencias a metodos de controllers).
 * Regla: toda ruta que no es publica declara sus roles al final. Cada persona agrega sus rutas en su bloque.
 * Los formularios HTML solo mandan GET y POST: borrar o editar es POST /x/{id}/eliminar, POST /x/{id}/editar.
 */
@Component
public class Router {

    private final AutorizacionMiddleware autorizacion;
    private final HomeController home;
    private final SesionController sesion;
    private final RegistroController registro;
    private final PanelController panel;

    public Router(AutorizacionMiddleware autorizacion, HomeController home, SesionController sesion,
            RegistroController registro, PanelController panel) {
        this.autorizacion = autorizacion;
        this.home = home;
        this.sesion = sesion;
        this.registro = registro;
        this.panel = panel;
    }

    public void registrar(Javalin app) {
        autorizacion.registrar(app);

        // --- Publico (A) ---
        app.get("/", home::index);
        app.get("/legal", home::legal);
        app.get("/login", sesion::mostrarLogin);
        app.post("/login", sesion::login);
        app.post("/logout", sesion::logout);
        app.get("/registro", registro::selector);
        app.get("/registro/persona-humana", registro::formPersonaHumana);
        app.post("/registro/persona-humana", registro::registrarPersonaHumana);
        app.get("/registro/persona-juridica", registro::formPersonaJuridica);
        app.post("/registro/persona-juridica", registro::registrarPersonaJuridica);
        app.get("/registro/entidad", registro::formEntidad);
        app.post("/registro/entidad", registro::registrarEntidad);

        // --- Entidad beneficiaria (A) ---
        app.get("/entidad", panel::entidad, ENTIDAD_BENEFICIARIA);

        // --- Donante (B) ---
        app.get("/donante", panel::donante, DONANTE);

        // --- Administrador (C) ---
        app.get("/admin", panel::admin, ADMINISTRADOR);
    }
}
