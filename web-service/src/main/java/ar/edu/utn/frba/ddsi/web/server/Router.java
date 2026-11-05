package ar.edu.utn.frba.ddsi.web.server;

import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.CHOFER;
import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.DONANTE;
import static ar.edu.utn.frba.ddsi.web.sesion.RolUsuario.ENTIDAD_BENEFICIARIA;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;


import ar.edu.utn.frba.ddsi.web.controllers.CamionesAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.CategoriasAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.ChoferWebController;
import ar.edu.utn.frba.ddsi.web.controllers.DepositoAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.MatchmakingAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.RecepcionAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.RutasAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.ImportacionesAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.DonacionesAsignadasController;
import ar.edu.utn.frba.ddsi.web.controllers.DonanteWebController;
import ar.edu.utn.frba.ddsi.web.controllers.DonantesAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.HomeController;
import ar.edu.utn.frba.ddsi.web.controllers.MapaController;
import ar.edu.utn.frba.ddsi.web.controllers.NecesidadesController;
import ar.edu.utn.frba.ddsi.web.controllers.PanelController;
import ar.edu.utn.frba.ddsi.web.controllers.RecepcionController;
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
    private final NecesidadesController necesidades;
    private final DonacionesAsignadasController donacionesAsignadas;
    private final RecepcionController recepcion;
    private final MapaController mapa;
    private final DonanteWebController donanteWeb;
    private final CamionesAdminController camionesAdmin;
    private final DepositoAdminController depositoAdmin;
    private final MatchmakingAdminController matchmakingAdmin;
    private final RecepcionAdminController recepcionAdmin;
    private final ImportacionesAdminController importacionesAdmin;
    private final CategoriasAdminController categoriasAdmin;
    private final DonantesAdminController donantesAdmin;
    private final RutasAdminController rutasAdmin;
    private final ChoferWebController choferWeb;

    @Autowired
    public Router(AutorizacionMiddleware autorizacion, HomeController home, SesionController sesion,
            RegistroController registro, PanelController panel, NecesidadesController necesidades,
            DonacionesAsignadasController donacionesAsignadas, RecepcionController recepcion, MapaController mapa,
            DonanteWebController donanteWeb,
            @Autowired(required = false) CamionesAdminController camionesAdmin,
            @Autowired(required = false) DepositoAdminController depositoAdmin,
            @Autowired(required = false) MatchmakingAdminController matchmakingAdmin,
            @Autowired(required = false) RecepcionAdminController recepcionAdmin,
            @Autowired(required = false) ImportacionesAdminController importacionesAdmin,
            @Autowired(required = false) CategoriasAdminController categoriasAdmin,
            @Autowired(required = false) DonantesAdminController donantesAdmin,
            @Autowired(required = false) RutasAdminController rutasAdmin,
            @Autowired(required = false) ChoferWebController choferWeb) {

        this.autorizacion = autorizacion;
        this.home = home;
        this.sesion = sesion;
        this.registro = registro;
        this.panel = panel;
        this.necesidades = necesidades;
        this.donacionesAsignadas = donacionesAsignadas;
        this.recepcion = recepcion;
        this.mapa = mapa;
        this.donanteWeb = donanteWeb;
        this.camionesAdmin = camionesAdmin;
        this.depositoAdmin = depositoAdmin;
        this.matchmakingAdmin = matchmakingAdmin;
        this.recepcionAdmin = recepcionAdmin;
        this.importacionesAdmin = importacionesAdmin;
        this.categoriasAdmin = categoriasAdmin;
        this.donantesAdmin = donantesAdmin;
        this.rutasAdmin = rutasAdmin;
        this.choferWeb = choferWeb;
    }

    public Router(AutorizacionMiddleware autorizacion, HomeController home, SesionController sesion,
            RegistroController registro, PanelController panel, NecesidadesController necesidades,
            DonacionesAsignadasController donacionesAsignadas, RecepcionController recepcion, MapaController mapa,
            DonanteWebController donanteWeb,
            CamionesAdminController camionesAdmin,
            DepositoAdminController depositoAdmin,
            MatchmakingAdminController matchmakingAdmin,
            RecepcionAdminController recepcionAdmin,
            ImportacionesAdminController importacionesAdmin,
            CategoriasAdminController categoriasAdmin) {
        this(autorizacion, home, sesion, registro, panel, necesidades, donacionesAsignadas, recepcion, mapa,
                donanteWeb, camionesAdmin, depositoAdmin, matchmakingAdmin, recepcionAdmin, importacionesAdmin,
                categoriasAdmin, null, null, null);
    }

  // Sobrecarga para tests
    public Router(AutorizacionMiddleware autorizacion, HomeController home, SesionController sesion,
                  RegistroController registro, PanelController panel, NecesidadesController necesidades,
                  DonacionesAsignadasController donacionesAsignadas, RecepcionController recepcion, MapaController mapa) {
      this(autorizacion, home, sesion, registro, panel, necesidades, donacionesAsignadas, recepcion, mapa,
              null, null, null, null, null, null, null, null, null, null);
    }

    public void registrar(Javalin app) {
        autorizacion.registrar(app);

        // --- Publico (A) ---
        // con sesion, landing, login y registro llevan al panel; /legal se ve con o sin sesion
        for (String ruta : new String[] {"/", "/login", "/registro", "/registro/*"}) {
            app.before(ruta, autorizacion::soloVisitantes);
        }
        app.get("/", home::index);
        app.get("/legal", home::legal);
        app.get("/mapa", mapa::mostrar);
        app.get("/direcciones/ubicar", mapa::ubicarDireccion); // mapa del formulario de registro de la entidad
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

        // --- Entidad beneficiaria (A + B) ---
        app.get("/entidad", panel::entidad, ENTIDAD_BENEFICIARIA);
        app.get("/entidad/necesidades", necesidades::listar, ENTIDAD_BENEFICIARIA);
        app.post("/entidad/necesidades", necesidades::crear, ENTIDAD_BENEFICIARIA);
        app.post("/entidad/necesidades/{id}/eliminar", necesidades::eliminar, ENTIDAD_BENEFICIARIA);
        app.get("/entidad/donaciones", donacionesAsignadas::listar, ENTIDAD_BENEFICIARIA);
        app.get("/entidad/donaciones/{id}/recepcion", recepcion::mostrar, ENTIDAD_BENEFICIARIA);
        app.post("/entidad/donaciones/{id}/recepcion", recepcion::confirmar, ENTIDAD_BENEFICIARIA);
        if( donanteWeb != null ) {
          app.get("/entidad/notificaciones", donanteWeb::notificacionesEntidad, ENTIDAD_BENEFICIARIA);
          app.get("/entidad/seguimiento", donanteWeb::seguimientoEntidad, ENTIDAD_BENEFICIARIA);
        }

        // --- Donante (B) ---
        app.get("/donante", panel::donante, DONANTE);
        if(donanteWeb != null) {
          app.get("/donante/donaciones", donanteWeb::misDonaciones, DONANTE);
          app.get("/donante/entidades", donanteWeb::entidades, DONANTE);
          app.get("/donante/notificaciones", donanteWeb::notificacionesDonante, DONANTE);
          app.get("/donante/seguimiento", donanteWeb::seguimientoDonante, DONANTE);
        }

        // --- Chofer (Logística) ---
        if (choferWeb != null) {
            app.get("/chofer", choferWeb::hojaDeRuta, CHOFER);
            app.post("/chofer/rutas/{rutaId}/iniciar", choferWeb::iniciarRuta, CHOFER);
            app.post("/chofer/rutas/{rutaId}/paradas/{paradaId}/confirmar", choferWeb::confirmarParada, CHOFER);
            app.post("/chofer/rutas/{rutaId}/paradas/{paradaId}/fallar", choferWeb::fallarParada, CHOFER);
        }

        // --- Administrador (C) ---
        app.get("/admin", panel::admin, ADMINISTRADOR);
        if (camionesAdmin != null) {
            app.get("/admin/camiones", camionesAdmin::listar, ADMINISTRADOR);
            app.post("/admin/camiones", camionesAdmin::crear, ADMINISTRADOR);
            app.post("/admin/camiones/{id}/editar", camionesAdmin::actualizar, ADMINISTRADOR);
            app.post("/admin/camiones/{id}/eliminar", camionesAdmin::eliminar, ADMINISTRADOR);
        }
        if (rutasAdmin != null) {
            app.get("/admin/rutas", rutasAdmin::listar, ADMINISTRADOR);
            app.post("/admin/rutas/planificar", rutasAdmin::planificar, ADMINISTRADOR);
        }
        if (depositoAdmin != null) {
            app.get("/admin/deposito", depositoAdmin::listar, ADMINISTRADOR);
            app.post("/admin/deposito/{id}/vencer", depositoAdmin::marcarVencida, ADMINISTRADOR);
            app.post("/admin/deposito/{id}/reprogramar", depositoAdmin::reprogramar, ADMINISTRADOR);
            app.post("/admin/deposito/{id}/reingresar", depositoAdmin::reingresar, ADMINISTRADOR);
        }
        if (matchmakingAdmin != null) {
            app.get("/admin/matchmaking", matchmakingAdmin::listar, ADMINISTRADOR);
            app.post("/admin/matchmaking/ejecutar", matchmakingAdmin::ejecutar, ADMINISTRADOR);
            app.post("/admin/matchmaking/propuestas/{id}/aceptar", matchmakingAdmin::aceptar, ADMINISTRADOR);
            app.post("/admin/matchmaking/propuestas/{id}/rechazar", matchmakingAdmin::rechazar, ADMINISTRADOR);
        }
        if (recepcionAdmin != null) {
            app.get("/admin/recepcion", recepcionAdmin::mostrarFormulario, ADMINISTRADOR);
            app.post("/admin/recepcion", recepcionAdmin::registrarDonacion, ADMINISTRADOR);
            app.post("/admin/recepcion/donante", recepcionAdmin::crearDonante, ADMINISTRADOR);
            app.post("/admin/recepcion/donante-humana", recepcionAdmin::crearDonanteHumana, ADMINISTRADOR);
        }
        if (importacionesAdmin != null) {
            app.get("/admin/importaciones", importacionesAdmin::listar, ADMINISTRADOR);
            app.post("/admin/importaciones", importacionesAdmin::importar, ADMINISTRADOR);
            app.get("/admin/importaciones/{id}/estado", importacionesAdmin::consultarEstado, ADMINISTRADOR);
        }
        if (donantesAdmin != null) {
            app.get("/admin/donantes", donantesAdmin::listar, ADMINISTRADOR);
            app.post("/admin/donantes", donantesAdmin::crear, ADMINISTRADOR);
            app.post("/admin/donantes/{id}/editar", donantesAdmin::actualizar, ADMINISTRADOR);
            app.post("/admin/donantes/{id}/eliminar", donantesAdmin::eliminar, ADMINISTRADOR);
        }
        if (categoriasAdmin != null) {
            app.get("/admin/categorias", categoriasAdmin::listar, ADMINISTRADOR);
            app.post("/admin/categorias", categoriasAdmin::crear, ADMINISTRADOR);
            app.post("/admin/categorias/{id}/editar", categoriasAdmin::actualizar, ADMINISTRADOR);
            app.post("/admin/categorias/{id}/subcategorias", categoriasAdmin::crearSubcategoria, ADMINISTRADOR);
            app.post("/admin/subcategorias/{id}/editar", categoriasAdmin::actualizarSubcategoria, ADMINISTRADOR);
            app.post("/admin/categorias/{id}/eliminar", categoriasAdmin::eliminar, ADMINISTRADOR);
            app.post("/admin/subcategorias/{id}/eliminar", categoriasAdmin::eliminarSubcategoria, ADMINISTRADOR);
        }
    }
}
