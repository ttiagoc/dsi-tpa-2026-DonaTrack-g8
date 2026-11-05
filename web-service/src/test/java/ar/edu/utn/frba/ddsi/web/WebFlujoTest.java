package ar.edu.utn.frba.ddsi.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.edu.utn.frba.ddsi.web.clients.ApiClient;
import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient;
import ar.edu.utn.frba.ddsi.web.config.WebConfig;
import ar.edu.utn.frba.ddsi.web.controllers.CamionesAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.CategoriasAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.ChoferWebController;
import ar.edu.utn.frba.ddsi.web.controllers.DepositoAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.DonantesAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.MatchmakingAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.RecepcionAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.RutasAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.ImportacionesAdminController;
import ar.edu.utn.frba.ddsi.web.controllers.DonacionesAsignadasController;
import ar.edu.utn.frba.ddsi.web.controllers.HomeController;
import ar.edu.utn.frba.ddsi.web.controllers.MapaController;
import ar.edu.utn.frba.ddsi.web.controllers.NecesidadesController;
import ar.edu.utn.frba.ddsi.web.controllers.PanelController;
import ar.edu.utn.frba.ddsi.web.controllers.RecepcionController;
import ar.edu.utn.frba.ddsi.web.controllers.RegistroController;
import ar.edu.utn.frba.ddsi.web.controllers.SesionController;
import ar.edu.utn.frba.ddsi.web.exceptions.NoEncontradoException;
import ar.edu.utn.frba.ddsi.web.fotos.AlmacenFotos;
import ar.edu.utn.frba.ddsi.web.middlewares.AutorizacionMiddleware;
import ar.edu.utn.frba.ddsi.web.server.Router;
import ar.edu.utn.frba.ddsi.web.server.Server;
import io.javalin.Javalin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Levanta el web-service real contra un donaciones-service falso (otro Javalin con /api/auth)
 * y recorre las pantallas como lo haria un navegador: cookies, redirects y formularios.
 */
@DisplayName("web-service: sesion, roles y registro")
class WebFlujoTest {

    private static final int PUERTO_API = 18090;
    private static final int PUERTO_WEB = 18091;
    private static final String WEB = "http://localhost:" + PUERTO_WEB;

    private final HttpClient navegador = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> ultimoRegistro = new AtomicReference<>();
    // estado de la API falsa de necesidades (entidad con perfilId 10)
    private final List<Map<String, Object>> necesidades = new ArrayList<>();
    private final AtomicReference<JsonNode> ultimaNecesidad = new AtomicReference<>();
    private final AtomicReference<String> necesidadEliminada = new AtomicReference<>();
    // donaciones asignadas a la entidad 10 que devuelve la API falsa
    private final List<Map<String, Object>> donacionesAsignadas = new ArrayList<>();
    private final AtomicReference<String> filtroDonaciones = new AtomicReference<>();
    private final AtomicReference<JsonNode> ultimaRecepcion = new AtomicReference<>();
    // lo que devuelve GET /api/donaciones/entregadas (mapa publico)
    private final List<Map<String, Object>> entregas = new ArrayList<>();
    private final List<Map<String, Object>> propuestasFalsas = new ArrayList<>();
    private final List<Map<String, Object>> donantesFalsos = new ArrayList<>();
    // GET /api/donantes/{id}/detalle y lo que recibe la API falsa al editar o borrar un donante
    private final Map<Long, Map<String, Object>> detallesDonantes = new HashMap<>();
    private final AtomicReference<JsonNode> ultimoDonanteEditado = new AtomicReference<>();
    private final AtomicReference<String> donanteEliminado = new AtomicReference<>();
    // ultimo pedido del ABM de categorias a la API falsa
    private final AtomicReference<JsonNode> ultimaCategoria = new AtomicReference<>();
    private final AtomicReference<String> ultimaSubcategoria = new AtomicReference<>();
    private final List<Map<String, Object>> choferesFalsos = new ArrayList<>();
    private final List<Map<String, Object>> rutasFalsas = new ArrayList<>();
    private final AtomicReference<String> ultimoMotivoFallo = new AtomicReference<>();

    @TempDir
    Path carpetaFotos;

    private Javalin apiFalsa;
    private Javalin web;

    @BeforeEach
    void levantar() {
        apiFalsa = crearApiFalsa().start(PUERTO_API);

        WebConfig config = new WebConfig();
        config.getSesion().setMinutosInactividad(30);
        config.getFotos().setDirectorio(carpetaFotos.toString());
        AlmacenFotos fotos = new AlmacenFotos(config);
        AuthApiClient auth = new AuthApiClient("http://localhost:" + PUERTO_API + "/api",
                HttpClient.newHttpClient(), ApiClient.crearObjectMapper());
        DonacionesApiClient donaciones = new DonacionesApiClient("http://localhost:" + PUERTO_API + "/api",
                HttpClient.newHttpClient(), ApiClient.crearObjectMapper(), auth);
        LogisticaApiClient logistica = new LogisticaApiClient("http://localhost:" + PUERTO_API + "/api",
                HttpClient.newHttpClient(), ApiClient.crearObjectMapper(), auth);
        CamionesAdminController camionesAdmin = new CamionesAdminController(logistica, auth);
        DepositoAdminController depositoAdmin = new DepositoAdminController(donaciones);
        MatchmakingAdminController matchmakingAdmin = new MatchmakingAdminController(donaciones);
        RecepcionAdminController recepcionAdmin = new RecepcionAdminController(donaciones);
        ImportacionesAdminController importacionesAdmin = new ImportacionesAdminController(donaciones);
        CategoriasAdminController categoriasAdmin = new CategoriasAdminController(donaciones);
        RutasAdminController rutasAdmin = new RutasAdminController(logistica, donaciones, auth);
        ChoferWebController choferWeb = new ChoferWebController(logistica, donaciones);
        Router router = new Router(new AutorizacionMiddleware(), new HomeController(), new SesionController(auth),
                new RegistroController(auth), new PanelController(), new NecesidadesController(donaciones),
                new DonacionesAsignadasController(donaciones), new RecepcionController(donaciones, fotos),
                new MapaController(donaciones), null, camionesAdmin, depositoAdmin, matchmakingAdmin, recepcionAdmin,
                importacionesAdmin, categoriasAdmin, new DonantesAdminController(donaciones), rutasAdmin, choferWeb);

        web = new Server(router, config, fotos).crear();
        // ruta solo para el test: una llamada autenticada a la API, para probar el refresh del token
        web.get("/test/me", ctx -> ctx.result(auth.me(ctx).username()));
        web.get("/test/no-encontrado", ctx -> {
            throw new NoEncontradoException("No existe la donación 99");
        });
        web.start(PUERTO_WEB);
    }

    @AfterEach
    void detener() {
        web.stop();
        apiFalsa.stop();
    }

    @Test
    @DisplayName("La landing y el CSS son publicos y no crean sesion")
    void landingPublica() throws Exception {
        HttpResponse<String> landing = get("/", null);
        assertEquals(200, landing.statusCode());
        assertTrue(landing.body().contains("Cómo funciona"));
        assertTrue(landing.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        assertTrue(landing.headers().allValues("Set-Cookie").isEmpty(), "un visitante no deberia recibir cookie");

        HttpResponse<String> css = get("/css/styles.css", null);
        assertEquals(200, css.statusCode());
        assertTrue(css.body().contains("--color-primario"));
    }

    @Test
    @DisplayName("Una ruta que no existe muestra la pagina 404 con el layout")
    void paginaNoEncontrada() throws Exception {
        HttpResponse<String> respuesta = get("/no-existe", null);
        assertEquals(404, respuesta.statusCode());
        assertTrue(respuesta.body().contains("No encontramos esta página"));
        assertTrue(respuesta.body().contains("navbar"));

        HttpResponse<String> conMensaje = get("/test/no-encontrado", null);
        assertEquals(404, conMensaje.statusCode());
        assertTrue(conMensaje.body().contains("No existe la donación 99"), "el mensaje del controller no se pisa");
    }

    @Test
    @DisplayName("Sin sesion, una pagina protegida redirige al login")
    void sinSesionRedirigeAlLogin() throws Exception {
        HttpResponse<String> respuesta = get("/donante", null);
        assertEquals(302, respuesta.statusCode());
        assertEquals("/login", respuesta.headers().firstValue("Location").orElse(null));
    }

    @Test
    @DisplayName("El login guarda la sesion en una cookie HttpOnly y SameSite y lleva al panel del rol")
    void loginLlevaAlPanel() throws Exception {
        HttpResponse<String> login = login("donante", "clave-segura");
        assertEquals(302, login.statusCode());
        assertEquals("/donante", login.headers().firstValue("Location").orElse(null));

        String setCookie = login.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("donatrack_session=")).findFirst().orElse(null);
        assertNotNull(setCookie, "falta la cookie de sesion");
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
        assertTrue(setCookie.toLowerCase().contains("samesite=lax"), setCookie);

        HttpResponse<String> panel = get("/donante", cookie(login));
        assertEquals(200, panel.statusCode());
        assertTrue(panel.body().contains("Hola, donante"));
        assertTrue(panel.body().contains("¡Hola, donante!"), "falta la alerta de bienvenida");

        HttpResponse<String> deNuevo = get("/donante", cookie(login));
        assertFalse(deNuevo.body().contains("¡Hola, donante!"), "la alerta se muestra una sola vez");
    }

    @Test
    @DisplayName("Cada rol cae en su panel")
    void cadaRolEnSuPanel() throws Exception {
        assertEquals("/entidad", login("entidad", "clave-segura").headers().firstValue("Location").orElse(null));
        assertEquals("/admin", login("admin", "clave-segura").headers().firstValue("Location").orElse(null));
        assertEquals("/chofer", login("chofer", "clave-segura").headers().firstValue("Location").orElse(null));
    }

    @Test
    @DisplayName("Con credenciales invalidas vuelve al formulario con el error y el usuario cargado")
    void loginInvalido() throws Exception {
        HttpResponse<String> respuesta = login("donante", "incorrecta");
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Usuario o contraseña incorrectos"));
        assertTrue(respuesta.body().contains("value=\"donante\""));
        assertFalse(respuesta.body().contains("incorrecta"), "la contrasenia no vuelve a la vista");
    }

    @Test
    @DisplayName("Con sesion, el panel usa el menu lateral y la barra superior en lugar del navbar")
    void layoutConSesion() throws Exception {
        String cookie = cookie(login("entidad", "clave-segura"));
        String panel = get("/entidad", cookie).body();
        assertTrue(panel.contains("class=\"menu-lateral\""));
        assertFalse(panel.contains("barra__titulo"), "la barra no repite el titulo de la pagina");
        assertTrue(panel.contains("href=\"/mapa\" title=\"Mapa de entregas\""), "el mapa publico esta en el menu");
        assertTrue(panel.contains("class=\"footer\""), "footer al pie del contenido");
        assertTrue(panel.contains("Entidad beneficiaria"), "rol en el perfil");
        assertTrue(panel.contains("href=\"/entidad\" title=\"Inicio\" aria-current=\"page\""), "marca la pagina abierta");
        assertTrue(panel.contains("for=\"menu-plegado\""), "boton para achicar el menu");
        assertTrue(panel.contains("Notificaciones"));
        assertTrue(panel.contains("action=\"/logout\""));
        assertFalse(panel.contains("class=\"navbar\""));
    }

    @Test
    @DisplayName("Los terminos son la misma pagina con o sin sesion: solo cambia el layout")
    void terminosConYSinSesion() throws Exception {
        HttpResponse<String> visitante = get("/legal", null);
        assertEquals(200, visitante.statusCode());
        assertTrue(visitante.body().contains("Términos de uso y privacidad"));
        assertTrue(visitante.body().contains("class=\"navbar\""));
        assertFalse(visitante.body().contains("class=\"menu-lateral\""));

        HttpResponse<String> logueado = get("/legal", cookie(login("donante", "clave-segura")));
        assertEquals(200, logueado.statusCode());
        assertTrue(logueado.body().contains("Términos de uso y privacidad"));
        assertTrue(logueado.body().contains("class=\"menu-lateral\""));
        assertFalse(logueado.body().contains("class=\"navbar\""));
    }

    @Test
    @DisplayName("Con sesion, la landing, el login y el registro llevan al panel")
    void paginasDeVisitanteRedirigenAlPanel() throws Exception {
        String cookie = cookie(login("donante", "clave-segura"));
        for (String ruta : new String[] {"/", "/login", "/registro", "/registro/entidad"}) {
            HttpResponse<String> respuesta = get(ruta, cookie);
            assertEquals(302, respuesta.statusCode(), ruta);
            assertEquals("/donante", respuesta.headers().firstValue("Location").orElse(null), ruta);
        }
        HttpResponse<String> registro = post("/registro/persona-humana", formularioPersona("otra"), cookie);
        assertEquals(302, registro.statusCode());
        assertEquals(null, ultimoRegistro.get(), "con sesion no se registra otra cuenta");
    }

    @Test
    @DisplayName("Necesidades: sin datos muestra la lista vacia y marca la pagina en el menu")
    void necesidadesVacias() throws Exception {
        HttpResponse<String> respuesta = get("/entidad/necesidades", cookie(login("entidad", "clave-segura")));
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Todavía no cargaste necesidades"));
        assertTrue(respuesta.body().contains("href=\"/entidad/necesidades\" title=\"Necesidades\" aria-current=\"page\""));
        assertTrue(respuesta.body().contains("<optgroup label=\"Ropa y abrigo\">"), "select agrupado por categoria");
        assertFalse(respuesta.body().contains("<dialog class=\"modal\" id=\"modal-alta\" aria-labelledby=\"titulo-alta\" open"));
    }

    @Test
    @DisplayName("Necesidades: el alta traduce la frecuencia a tipo + periodo y vuelve al listado")
    void necesidadAlta() throws Exception {
        String cookie = cookie(login("entidad", "clave-segura"));
        HttpResponse<String> alta = post("/entidad/necesidades", Map.of("subcategoria", "Frazadas",
                "frecuencia", "SEMANAL", "cantidad", "30", "unidad", "KILOGRAMOS", "descripcion", "Para el refugio"), cookie);
        assertEquals(302, alta.statusCode(), alta.body());
        assertEquals("/entidad/necesidades", alta.headers().firstValue("Location").orElse(null));

        JsonNode enviado = ultimaNecesidad.get();
        assertEquals("Frazadas", enviado.get("subcategoria").get("nombre").asText());
        assertEquals("recurrente", enviado.get("tipoNecesidad").asText());
        assertEquals("SEMANAL", enviado.get("periodo").asText());
        assertEquals(30, enviado.get("cantidad").asLong());
        assertEquals("KILOGRAMOS", enviado.get("unidad").asText());

        String listado = get("/entidad/necesidades", cookie).body();
        assertTrue(listado.contains("Necesidad agregada."));
        assertTrue(listado.contains("Todas las semanas"));
        assertTrue(listado.contains("12 de 30 kg"), "cantidad con su unidad");
        assertTrue(listado.contains("width: 40%"), "barra de progreso");
        assertTrue(listado.contains("Pendiente"));
    }

    @Test
    @DisplayName("Necesidades: con datos invalidos vuelve con el modal abierto, los errores y lo cargado")
    void necesidadInvalida() throws Exception {
        HttpResponse<String> respuesta = post("/entidad/necesidades", Map.of("subcategoria", "Arroz",
                "frecuencia", "", "cantidad", "0", "unidad", "", "descripcion", "Almuerzos"), cookie(login("entidad", "clave-segura")));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("id=\"modal-alta\" aria-labelledby=\"titulo-alta\" open"));
        assertTrue(respuesta.body().contains("Elegí cada cuánto la necesitan"));
        assertTrue(respuesta.body().contains("Tiene que ser un número mayor a 0"));
        assertTrue(respuesta.body().contains("Elegí en qué se mide la cantidad"));
        assertTrue(respuesta.body().contains("<option selected>Arroz</option>"), "conserva la subcategoria");
        assertTrue(respuesta.body().contains(">Almuerzos</textarea>"), "conserva la descripcion");
        assertEquals(null, ultimaNecesidad.get(), "no deberia haber llamado a la API");
    }

    @Test
    @DisplayName("Necesidades: el error de la API se muestra dentro del modal")
    void necesidadErrorDeLaApi() throws Exception {
        HttpResponse<String> respuesta = post("/entidad/necesidades", Map.of("subcategoria", "Inexistente",
                "frecuencia", "UNA_VEZ", "cantidad", "5", "unidad", "UNIDADES", "descripcion", "x"), cookie(login("entidad", "clave-segura")));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("No existe la subcategoria"));
        assertEquals("extraordinaria", ultimaNecesidad.get().get("tipoNecesidad").asText());
        assertTrue(ultimaNecesidad.get().get("periodo").isNull());
    }

    @Test
    @DisplayName("Necesidades: eliminar llama a la API y vuelve al listado con un aviso")
    void necesidadEliminar() throws Exception {
        String cookie = cookie(login("entidad", "clave-segura"));
        HttpResponse<String> respuesta = post("/entidad/necesidades/5/eliminar", Map.of(), cookie);
        assertEquals(302, respuesta.statusCode());
        assertEquals("5", necesidadEliminada.get());
        assertTrue(get("/entidad/necesidades", cookie).body().contains("Necesidad eliminada."));
    }

    @Test
    @DisplayName("Necesidades: con donaciones en camino, eliminar avisa que no se puede y muestra el error de la API")
    void necesidadConDonacionesEnCurso() throws Exception {
        Map<String, Object> necesidad = new LinkedHashMap<>();
        necesidad.put("id", 7);
        necesidad.put("subcategoria", "Leche larga vida");
        necesidad.put("periodo", "SEMANAL");
        necesidad.put("descripcion", "Desayuno");
        necesidad.put("cantidad", 40);
        necesidad.put("cantidadAsignada", 12);
        necesidad.put("satisfecha", false);
        necesidad.put("donacionesEnCurso", 1);
        necesidades.add(necesidad);
        String cookie = cookie(login("entidad", "clave-segura"));

        String listado = get("/entidad/necesidades", cookie).body();
        assertTrue(listado.contains("data-en-curso=\"1\""));
        assertTrue(listado.contains("id=\"modal-no-eliminar\""));
        assertTrue(listado.contains("Las donaciones que ya llegaron siguen apareciendo en Donaciones asignadas"));

        // sin JavaScript el formulario se manda igual: la API lo rechaza y vuelve con el error
        assertEquals(302, post("/entidad/necesidades/7/eliminar", Map.of(), cookie).statusCode());
        assertTrue(get("/entidad/necesidades", cookie).body()
                .contains("No se pudo eliminar la necesidad: No se puede eliminar la necesidad: tiene 1 donación"));
    }

    @Test
    @DisplayName("Donaciones asignadas: sin datos muestra la lista vacia y pide las de la entidad logueada")
    void donacionesAsignadasVacias() throws Exception {
        HttpResponse<String> respuesta = get("/entidad/donaciones", cookie(login("entidad", "clave-segura")));
        assertEquals(200, respuesta.statusCode());
        assertEquals("10", filtroDonaciones.get());
        assertTrue(respuesta.body().contains("Todavía no te asignaron donaciones."));
        assertTrue(respuesta.body().contains("href=\"/entidad/donaciones\" title=\"Asignadas\" aria-current=\"page\""));
        assertFalse(respuesta.body().contains("<table"));
    }

    @Test
    @DisplayName("Donaciones asignadas: cada estado en palabras, el resumen y confirmar solo en las entregadas sin fotos")
    void donacionesAsignadas() throws Exception {
        donacionesAsignadas.add(donacion(121, "ENTREGADA", List.of(), "Arroz", "Almuerzo de lunes a viernes"));
        donacionesAsignadas.add(donacion(128, "EN_TRASLADO", List.of(), "Leche larga vida", "Desayuno"));
        donacionesAsignadas.add(donacion(132, "ASIGNACION_REALIZADA", List.of(), "Frazadas", "Refugio"));
        donacionesAsignadas.add(donacion(98, "ENTREGA_FALLIDA", List.of(), "Arroz", "Almuerzo de lunes a viernes"));
        donacionesAsignadas.add(donacion(104, "ENTREGADA", List.of("a.jpg", "b.jpg", "c.jpg"), null, null));

        String pagina = get("/entidad/donaciones", cookie(login("entidad", "clave-segura"))).body();

        assertTrue(pagina.contains("<strong>5</strong>Asignadas"));
        assertTrue(pagina.contains("<strong>3</strong>Por llegar"));
        assertTrue(pagina.contains("<strong>1</strong>Para confirmar"));
        assertTrue(pagina.contains("<strong>1</strong>Recibidas"));
        assertTrue(pagina.contains("Asignada el 29/10"));
        assertTrue(pagina.contains("Arroz largo fino × 60"));
        assertTrue(pagina.contains("title=\"Almuerzo de lunes a viernes\""));
        assertTrue(pagina.contains("etiqueta--info\">En camino"));
        assertTrue(pagina.contains("etiqueta--error\">Entrega fallida"));
        assertTrue(pagina.contains("Esperando el envío"));
        assertTrue(pagina.contains("El 01/11 · falta confirmar"));
        assertTrue(pagina.contains("Entregada el 01/11 · 3 fotos"));
        assertTrue(pagina.contains("La necesidad se eliminó"), "la #104 ya no tiene necesidad");
        assertTrue(pagina.contains("href=\"/entidad/donaciones/121/recepcion\""));
        assertFalse(pagina.contains("/entidad/donaciones/104/recepcion"), "la recibida no se confirma de nuevo");
        assertFalse(pagina.contains("/entidad/donaciones/128/recepcion"), "la que esta en camino tampoco");
    }

    @Test
    @DisplayName("Donaciones asignadas: un donante no puede entrar")
    void donacionesAsignadasSoloEntidad() throws Exception {
        assertEquals(403, get("/entidad/donaciones", cookie(login("donante", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Mapa: es publico, marca la pagina en el navbar y sin entregas muestra el mensaje")
    void mapaVacio() throws Exception {
        HttpResponse<String> respuesta = get("/mapa", null);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Todavía no hay donaciones entregadas."));
        assertTrue(respuesta.body().contains("href=\"/mapa\" aria-current=\"page\""));
        assertTrue(get("/", null).body().contains("href=\"/mapa\""), "la landing enlaza al mapa (req. 1)");
    }

    @Test
    @DisplayName("Mapa: un marcador por entidad con su detalle y las fotos de las entregas")
    void mapaConEntregas() throws Exception {
        entregas.add(entidadConEntregas(6, "Comedor San Martín", -34.6283, -58.4639, List.of(
                entrega(2, "Arroz largo fino 1 kg", 60, List.of("/fotos/a.png", "/fotos/b.png")),
                entrega(3, "Leche entera 1 L", 12, List.of()))));
        entregas.add(entidadConEntregas(9, "Escuela Rural 10", null, null, List.of(
                entrega(7, "Kits escolares", 50, List.of("/fotos/c.png")))));

        String pagina = get("/mapa", null).body();

        assertTrue(pagina.contains("<strong>3</strong>Donaciones entregadas"));
        assertTrue(pagina.contains("<strong>2</strong>Entidades"));
        assertTrue(pagina.contains("aria-label=\"Comedor San Martín: 2 donaciones\" aria-current=\"true\""),
                "la primera entidad arranca elegida");
        assertTrue(pagina.contains("aria-label=\"Escuela Rural 10: 1 donación\""));
        assertTrue(pagina.contains("href=\"#entidad-9\""), "sin JavaScript, el marcador lleva al detalle");
        assertTrue(pagina.contains("<strong>Arroz largo fino 1 kg × 60</strong>"));
        assertTrue(pagina.contains("Entregada el 01/11 · Alimentos"));
        assertTrue(pagina.contains("<img src=\"/fotos/a.png\""));
        assertTrue(pagina.contains("· sin fotos todavía"));
        assertTrue(pagina.contains("leaflet.js"), "los marcadores van a un mapa de OpenStreetMap");
        assertTrue(pagina.contains("data-lat=\"-34.6283\" data-lng=\"-58.4639\""), "la entidad geocodificada va en su direccion");
        assertTrue(pagina.contains("mapa__marcador mapa__marcador--aproximado\" href=\"#entidad-9\""),
                "la que no tiene coordenadas cae en un punto aproximado, marcado como tal");
        assertTrue(pagina.contains("Ubicación aproximada"));
    }

    @Test
    @DisplayName("Mapa: con sesion se ve con el menu lateral, que tiene el link al mapa")
    void mapaConSesion() throws Exception {
        String pagina = get("/mapa", cookie(login("entidad", "clave-segura"))).body();
        assertTrue(pagina.contains("class=\"menu-lateral\""));
        assertTrue(pagina.contains("href=\"/mapa\" title=\"Mapa de entregas\" aria-current=\"page\""));
    }

    @Test
    @DisplayName("Recepcion: muestra la donacion y el campo de fotos")
    void recepcionFormulario() throws Exception {
        donacionesAsignadas.add(donacion(121, "ENTREGADA", List.of(), "Arroz", "Almuerzo de lunes a viernes"));
        HttpResponse<String> pagina = get("/entidad/donaciones/121/recepcion", cookie(login("entidad", "clave-segura")));
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("Arroz · Almuerzo de lunes a viernes"));
        assertTrue(pagina.body().contains("Arroz largo fino × 60"));
        assertTrue(pagina.body().contains("El 01/11 a las 15:30"));
        assertTrue(pagina.body().contains("enctype=\"multipart/form-data\""));
        assertTrue(pagina.body().contains("/js/fotos.js"));
    }

    @Test
    @DisplayName("Recepcion: guarda las fotos en el disco, manda sus URLs y quedan publicas")
    void recepcionConfirmada() throws Exception {
        donacionesAsignadas.add(donacion(121, "ENTREGADA", List.of(), "Arroz", "Almuerzo de lunes a viernes"));
        String cookie = cookie(login("entidad", "clave-segura"));

        Map<String, byte[]> fotos = new LinkedHashMap<>();
        fotos.put("bolsas.png", png());
        fotos.put("deposito.jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2});
        HttpResponse<String> respuesta = postFotos("/entidad/donaciones/121/recepcion", fotos, cookie);
        assertEquals(302, respuesta.statusCode(), respuesta.body());
        assertEquals("/entidad/donaciones", respuesta.headers().firstValue("Location").orElse(null));

        JsonNode urls = ultimaRecepcion.get().get("fotosUrl");
        assertEquals(2, urls.size());
        assertTrue(urls.get(0).asText().matches("/fotos/[0-9a-f-]{36}\\.png"), urls.toString());
        assertTrue(urls.get(1).asText().endsWith(".jpg"), "la extension sale del contenido");
        try (var guardadas = Files.list(carpetaFotos)) {
            assertEquals(2, guardadas.count());
        }
        assertEquals(200, get(urls.get(0).asText(), null).statusCode(), "las fotos se ven sin sesion");
        assertTrue(get("/entidad/donaciones", cookie).body().contains("Recepción de la donación #121 confirmada"));
    }

    @Test
    @DisplayName("Recepcion: sin fotos o con un archivo que no es foto vuelve con el error y no llama a la API")
    void recepcionInvalida() throws Exception {
        donacionesAsignadas.add(donacion(121, "ENTREGADA", List.of(), "Arroz", "Almuerzo de lunes a viernes"));
        String cookie = cookie(login("entidad", "clave-segura"));

        HttpResponse<String> sinFotos = postFotos("/entidad/donaciones/121/recepcion", Map.of(), cookie);
        assertEquals(400, sinFotos.statusCode());
        assertTrue(sinFotos.body().contains("Cargá al menos una foto de lo que recibieron"));
        assertTrue(sinFotos.body().contains("campo--fotos campo--error"));

        HttpResponse<String> noEsFoto = postFotos("/entidad/donaciones/121/recepcion",
                Map.of("virus.jpg", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)), cookie);
        assertEquals(400, noEsFoto.statusCode());
        assertTrue(noEsFoto.body().contains("no es una foto JPG o PNG"));

        assertEquals(null, ultimaRecepcion.get());
        try (var guardadas = Files.list(carpetaFotos)) {
            assertEquals(0, guardadas.count());
        }
    }

    @Test
    @DisplayName("Recepcion: si la API la rechaza, muestra el error y borra las fotos guardadas")
    void recepcionRechazadaPorLaApi() throws Exception {
        donacionesAsignadas.add(donacion(122, "ENTREGADA", List.of(), "Arroz", "Almuerzo de lunes a viernes"));
        HttpResponse<String> respuesta = postFotos("/entidad/donaciones/122/recepcion",
                Map.of("bolsas.png", png()), cookie(login("entidad", "clave-segura")));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("ya fue confirmada"));
        try (var guardadas = Files.list(carpetaFotos)) {
            assertEquals(0, guardadas.count());
        }
    }

    @Test
    @DisplayName("Recepcion: una donacion sin entregar o ya confirmada vuelve al listado con un aviso")
    void recepcionNoCorresponde() throws Exception {
        donacionesAsignadas.add(donacion(128, "EN_TRASLADO", List.of(), "Leche larga vida", "Desayuno"));
        donacionesAsignadas.add(donacion(104, "ENTREGADA", List.of("/fotos/a.jpg"), "Útiles escolares", "Kits"));
        String cookie = cookie(login("entidad", "clave-segura"));

        HttpResponse<String> enCamino = get("/entidad/donaciones/128/recepcion", cookie);
        assertEquals(302, enCamino.statusCode());
        assertTrue(get("/entidad/donaciones", cookie).body().contains("La donación #128 todavía no fue entregada"));

        assertEquals(302, postFotos("/entidad/donaciones/104/recepcion", Map.of("a.png", png()), cookie).statusCode());
        assertTrue(get("/entidad/donaciones", cookie).body().contains("La recepción de la donación #104 ya está confirmada"));
        assertEquals(null, ultimaRecepcion.get());
    }

    @Test
    @DisplayName("Necesidades: un donante no puede entrar")
    void necesidadesSoloEntidad() throws Exception {
        assertEquals(403, get("/entidad/necesidades", cookie(login("donante", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Con el rol equivocado se muestra la pagina 403")
    void rolEquivocado() throws Exception {
        String cookie = cookie(login("donante", "clave-segura"));
        HttpResponse<String> respuesta = get("/admin", cookie);
        assertEquals(403, respuesta.statusCode());
        assertTrue(respuesta.body().contains("No tenés acceso"));
    }

    @Test
    @DisplayName("El logout borra la sesion")
    void logout() throws Exception {
        String cookie = cookie(login("donante", "clave-segura"));

        HttpResponse<String> salida = post("/logout", Map.of(), cookie);
        assertEquals(302, salida.statusCode());
        assertEquals("/?logout", salida.headers().firstValue("Location").orElse(null));
        assertTrue(salida.headers().allValues("Set-Cookie").stream()
                .anyMatch(c -> c.startsWith("donatrack_session=") && c.contains("Max-Age=0")));

        assertEquals(302, get("/donante", cookie).statusCode());
    }

    @Test
    @DisplayName("Si el access token vencio, se renueva con el refresh token y se reintenta")
    void renuevaElToken() throws Exception {
        String cookie = cookie(login("donante", "clave-segura"));
        HttpResponse<String> respuesta = get("/test/me", cookie);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertEquals("donante", respuesta.body());
    }

    @Test
    @DisplayName("Si tambien vencio el refresh token, vuelve al login avisando")
    void sesionVencida() throws Exception {
        String cookie = cookie(login("vencido", "clave-segura"));
        HttpResponse<String> respuesta = get("/test/me", cookie);
        assertEquals(302, respuesta.statusCode());
        assertEquals("/login?vencida", respuesta.headers().firstValue("Location").orElse(null));
        assertEquals(302, get("/donante", cookie).statusCode(), "la sesion se cerro");
    }

    @Test
    @DisplayName("Registro de persona: valida los obligatorios sin llamar a la API")
    void registroValidaObligatorios() throws Exception {
        HttpResponse<String> respuesta = post("/registro/persona-humana",
                Map.of("nombre", "Ana", "password", "clave-segura", "password2", "otra-clave"), null);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("El apellido es obligatorio"));
        assertTrue(respuesta.body().contains("Las contraseñas no coinciden"));
        assertTrue(respuesta.body().contains("value=\"Ana\""));
        assertEquals(null, ultimoRegistro.get(), "no deberia haber llamado a la API");
    }

    @Test
    @DisplayName("Registro de persona: arma el JSON del backend y deja la sesion iniciada")
    void registroPersonaHumana() throws Exception {
        HttpResponse<String> respuesta = post("/registro/persona-humana", formularioPersona("ana"), null);
        assertEquals(302, respuesta.statusCode(), respuesta.body());
        assertEquals("/donante", respuesta.headers().firstValue("Location").orElse(null));

        JsonNode enviado = ultimoRegistro.get();
        assertEquals("ana", enviado.get("username").asText());
        JsonNode donante = enviado.get("donante");
        assertEquals("1990-05-04", donante.get("fechaNacimiento").asText());
        assertEquals("Peña", donante.get("apellido").asText(), "los acentos llegan bien");
        assertEquals(2, donante.get("contactos").size());
        assertEquals("WHATSAPP", donante.get("contactoPredeterminado").get("tipo").asText());

        assertEquals(200, get("/donante", cookie(respuesta)).statusCode());
    }

    @Test
    @DisplayName("Registro: valida el formato del email, del telefono y el largo de la contrasenia")
    void registroValidaFormatos() throws Exception {
        Map<String, String> form = formularioPersona("ana");
        form.put("email", "ana-sin-arroba");
        form.put("telefono", "hola");
        form.put("password", "corta");
        form.put("password2", "corta");
        HttpResponse<String> respuesta = post("/registro/persona-humana", form, null);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Ingresá un email válido"));
        assertTrue(respuesta.body().contains("Ingresá un teléfono válido"));
        assertTrue(respuesta.body().contains("Tiene que tener al menos 8 caracteres"));
        assertEquals(null, ultimoRegistro.get(), "no deberia haber llamado a la API");

        form = formularioPersona("ana");
        form.put("telefono", "+54 (11) 5555-6666");
        assertEquals(302, post("/registro/persona-humana", form, null).statusCode(), "formatos validos");
    }

    @Test
    @DisplayName("Los formularios cargan la validacion del navegador y el selector con buscador")
    void scriptsDeFormularios() throws Exception {
        String login = get("/login", null).body();
        assertTrue(login.contains("/js/formularios.js"));
        assertTrue(login.contains("/js/selector-buscador.js"));
        assertEquals(200, get("/js/formularios.js", null).statusCode());
        assertEquals(200, get("/js/selector-buscador.js", null).statusCode());
        String necesidades = get("/entidad/necesidades", cookie(login("entidad", "clave-segura"))).body();
        assertTrue(necesidades.contains("data-buscador="));
        assertTrue(necesidades.contains("min=\"1\""));
    }

    @Test
    @DisplayName("Registro de persona: muestra el error de negocio que devuelve la API")
    void registroErrorDeNegocio() throws Exception {
        HttpResponse<String> respuesta = post("/registro/persona-humana", formularioPersona("repetido"), null);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("El nombre de usuario repetido ya esta en uso"));
    }

    private Map<String, String> formularioEntidad(String calle, String altura) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("razonSocial", "Comedor Los Pibes");
        campos.put("calle", calle);
        campos.put("altura", altura);
        campos.put("localidad", "Flores");
        campos.put("provincia", "Ciudad Autónoma de Buenos Aires");
        campos.put("telefono", "1145678901");
        campos.put("correoRepresentante", "comedor@mail.com");
        campos.put("username", "comedor");
        campos.put("password", "clave-segura");
        campos.put("password2", "clave-segura");
        return campos;
    }

    @Test
    @DisplayName("Registro de entidad: la direccion se carga en campos separados y la API la recibe armada")
    void registroEntidadArmaLaDireccion() throws Exception {
        String formulario = get("/registro/entidad", null).body();
        assertTrue(formulario.contains("id=\"calle\"") && formulario.contains("id=\"altura\"")
                && formulario.contains("id=\"localidad\"") && formulario.contains("id=\"provincia\""));
        assertTrue(formulario.contains("<option>Tierra del Fuego</option>"));
        assertTrue(formulario.contains("/js/direccion.js"), "el mapa para verificar la dirección");

        HttpResponse<String> respuesta = post("/registro/entidad", formularioEntidad("Av. Rivadavia, ", "6500"), null);

        assertEquals(302, respuesta.statusCode());
        assertEquals("Av. Rivadavia 6500, Flores, Ciudad Autónoma de Buenos Aires",
                ultimoRegistro.get().get("entidad").get("direccion").asText(), "las comas de lo escrito se sacan");
    }

    @Test
    @DisplayName("Registro de entidad: valida la altura y la provincia sin llamar a la API")
    void registroEntidadValidaLaDireccion() throws Exception {
        Map<String, String> campos = formularioEntidad("Av. Rivadavia", "seis mil");
        campos.put("provincia", "Narnia");

        HttpResponse<String> respuesta = post("/registro/entidad", campos, null);

        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Ingresá solo el número de la calle"));
        assertTrue(respuesta.body().contains("Elegí una provincia de la lista"));
        assertEquals(null, ultimoRegistro.get(), "no deberia haber llamado a la API");
    }

    @Test
    @DisplayName("Registro de entidad: si la API no encuentra la direccion, el error queda en el recuadro de la direccion")
    void registroEntidadDireccionInexistente() throws Exception {
        HttpResponse<String> respuesta = post("/registro/entidad", formularioEntidad("Calle Inventada", "99999"), null);

        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("direccion direccion--error"));
        assertTrue(respuesta.body().contains("id=\"direccion-error\""));
        assertTrue(respuesta.body().contains("no existe o no la encontramos"));
        assertTrue(respuesta.body().contains("value=\"Calle Inventada\"") && respuesta.body().contains("value=\"99999\""),
                "se conserva lo escrito");
        assertTrue(respuesta.body().contains("<option selected>Ciudad Autónoma de Buenos Aires</option>"));
    }

    @Test
    @DisplayName("Ubicar direccion: devuelve el punto, o el mensaje si no existe o falta algo")
    void ubicarDireccion() throws Exception {
        HttpResponse<String> encontrada = get("/direcciones/ubicar?calle=Av.%20Rivadavia&altura=6500&localidad=Flores"
                + "&provincia=Ciudad%20Aut%C3%B3noma%20de%20Buenos%20Aires", null);
        assertEquals(200, encontrada.statusCode());
        JsonNode punto = mapper.readTree(encontrada.body());
        assertEquals(-34.6271, punto.get("latitud").asDouble());
        assertEquals("AV RIVADAVIA 6500, Comuna 7, Ciudad Autónoma de Buenos Aires", punto.get("normalizada").asText());

        HttpResponse<String> inexistente = get("/direcciones/ubicar?calle=Inventada&altura=1&localidad=Flores&provincia=CABA", null);
        assertEquals(400, inexistente.statusCode());
        assertTrue(mapper.readTree(inexistente.body()).get("mensaje").asText().contains("no existe"));

        assertEquals(400, get("/direcciones/ubicar?calle=Av.%20Rivadavia", null).statusCode(), "faltan datos");
    }

    @Test
    @DisplayName("Admin camiones: control de acceso por rol")
    void camionesSeguridad() throws Exception {
        assertEquals(302, get("/admin/camiones", null).statusCode());
        assertEquals(403, get("/admin/camiones", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/camiones", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/camiones", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin camiones: flujo de alta, edición y baja")
    void camionesFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // Ver listado
        HttpResponse<String> listado = get("/admin/camiones", adminCookie);
        assertEquals(200, listado.statusCode());
        assertTrue(listado.body().contains("Camiones"));
        assertTrue(listado.body().contains("action=\"/admin/camiones\""));
        assertFalse(listado.body().contains("action=\"/admin/camiones//editar\""));

        // Alta inválida (sin patente)
        Map<String, String> formInvalido = new LinkedHashMap<>();
        formInvalido.put("patente", "");
        formInvalido.put("capacidadCarga", "4500");
        formInvalido.put("capacidadVolumen", "30");
        formInvalido.put("altura", "2.8");
        formInvalido.put("choferNombre", "Carlos");
        formInvalido.put("choferApellido", "Gómez");
        HttpResponse<String> respError = post("/admin/camiones", formInvalido, adminCookie);
        assertEquals(400, respError.statusCode());
        assertTrue(respError.body().contains("La patente es obligatorio"));

        // Alta exitosa
        Map<String, String> formValido = new LinkedHashMap<>(formInvalido);
        formValido.put("patente", "AF 123 CD");
        HttpResponse<String> respAlta = post("/admin/camiones", formValido, adminCookie);
        assertEquals(302, respAlta.statusCode());
        assertEquals("/admin/camiones", respAlta.headers().firstValue("Location").orElse(null));

        // Edición exitosa
        Map<String, String> formEdicion = new LinkedHashMap<>(formValido);
        formEdicion.put("capacidadCarga", "5000");
        HttpResponse<String> respEdit = post("/admin/camiones/1/editar", formEdicion, adminCookie);
        assertEquals(302, respEdit.statusCode());

        // Eliminación exitosa
        HttpResponse<String> respDel = post("/admin/camiones/1/eliminar", Map.of(), adminCookie);
        assertEquals(302, respDel.statusCode());
    }

    @Test
    @DisplayName("Admin categorías: solo el administrador entra")
    void categoriasSeguridad() throws Exception {
        assertEquals(302, get("/admin/categorias", null).statusCode());
        assertEquals(403, get("/admin/categorias", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/categorias", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/categorias", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin categorías: alta y edición de categorías y subcategorías")
    void categoriasFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // listado: cada categoria con sus subcategorias, sin baja
        HttpResponse<String> listado = get("/admin/categorias", adminCookie);
        assertEquals(200, listado.statusCode());
        assertTrue(listado.body().contains("Ropa y abrigo"));
        assertTrue(listado.body().contains("/admin/categorias?editarSub=3"));
        assertTrue(listado.body().contains("/admin/categorias?nuevaSub=1"));
        assertTrue(listado.body().contains("/admin/categorias?eliminar=2"));

        // alta sin datos: vuelve con el modal abierto y los errores
        HttpResponse<String> sinDatos = post("/admin/categorias", Map.of("nombre", ""), adminCookie);
        assertEquals(400, sinDatos.statusCode());
        assertTrue(sinDatos.body().contains("El nombre es obligatorio"));
        assertTrue(sinDatos.body().contains("Indicá si la categoría es perecedera"));

        // nombre repetido: el mensaje de la API aparece en el modal y se conserva lo escrito
        HttpResponse<String> repetida = post("/admin/categorias",
                Map.of("nombre", "Alimentos", "pideEstado", "no", "esPerecedero", "si"), adminCookie);
        assertEquals(400, repetida.statusCode());
        assertTrue(repetida.body().contains("Ya existe una categoría con el nombre"));
        assertTrue(repetida.body().contains("value=\"Alimentos\""));

        // alta correcta
        HttpResponse<String> alta = post("/admin/categorias",
                Map.of("nombre", "Higiene", "pideEstado", "si", "esPerecedero", "no"), adminCookie);
        assertEquals(302, alta.statusCode());
        assertEquals("/admin/categorias", alta.headers().firstValue("Location").orElse(null));
        assertTrue(ultimaCategoria.get().get("pideEstado").asBoolean());
        assertFalse(ultimaCategoria.get().get("esPerecedero").asBoolean());

        // edicion: el modal viene con el action de la categoria
        HttpResponse<String> editar = get("/admin/categorias?editar=2", adminCookie);
        assertTrue(editar.body().contains("action=\"/admin/categorias/2/editar\""));
        assertEquals(302, post("/admin/categorias/2/editar",
                Map.of("nombre", "Ropa", "pideEstado", "si", "esPerecedero", "no"), adminCookie).statusCode());
        assertEquals("Ropa", ultimaCategoria.get().get("nombre").asText());

        // subcategorias: alta dentro de una categoria y renombre
        HttpResponse<String> nuevaSub = get("/admin/categorias?nuevaSub=1", adminCookie);
        assertTrue(nuevaSub.body().contains("action=\"/admin/categorias/1/subcategorias\""));
        assertEquals(302, post("/admin/categorias/1/subcategorias", Map.of("subcategoria", "Fideos"), adminCookie).statusCode());
        assertEquals("POST 1 Fideos", ultimaSubcategoria.get());

        HttpResponse<String> editarSub = get("/admin/categorias?editarSub=2", adminCookie);
        assertTrue(editarSub.body().contains("value=\"Arroz\""));
        assertEquals(400, post("/admin/subcategorias/2/editar", Map.of("subcategoria", " "), adminCookie).statusCode());
        assertEquals(302, post("/admin/subcategorias/2/editar", Map.of("subcategoria", "Arroz largo fino"), adminCookie).statusCode());
        assertEquals("PUT 2 Arroz largo fino", ultimaSubcategoria.get());
    }

    @Test
    @DisplayName("Admin categorías: solo se elimina lo que no está en uso")
    void categoriasBaja() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // categoria sin uso: confirma nombrando las subcategorias que tambien se borran
        HttpResponse<String> confirmar = get("/admin/categorias?eliminar=2", adminCookie);
        assertTrue(confirmar.body().contains("¿Eliminar la categoría?"));
        assertTrue(confirmar.body().contains("También se eliminan sus subcategorías"));
        assertTrue(confirmar.body().contains("action=\"/admin/categorias/2/eliminar\""));
        HttpResponse<String> baja = post("/admin/categorias/2/eliminar", Map.of(), adminCookie);
        assertEquals(302, baja.statusCode());
        assertEquals("DELETE categoria 2", ultimaSubcategoria.get());
        assertTrue(get("/admin/categorias", adminCookie).body().contains("Se eliminó la categoría con sus subcategorías."));

        // categoria en uso: explica por que no y no ofrece el boton de eliminar
        HttpResponse<String> enUso = get("/admin/categorias?eliminar=1", adminCookie);
        assertTrue(enUso.body().contains("No se puede eliminar la categoría"));
        assertTrue(enUso.body().contains("La usan 3 donaciones y 1 necesidad"));
        assertFalse(enUso.body().contains("action=\"/admin/categorias/1/eliminar\""));

        // si igual llega el POST, el motivo de la API se muestra como aviso
        post("/admin/categorias/1/eliminar", Map.of(), adminCookie);
        assertTrue(get("/admin/categorias", adminCookie).body().contains("la usan 3 donaciones y 1 necesidad"));

        // subcategoria: el modal de edicion lleva a eliminarla
        assertTrue(get("/admin/categorias?editarSub=3", adminCookie).body().contains("/admin/categorias?eliminarSub=3"));
        assertTrue(get("/admin/categorias?eliminarSub=3", adminCookie).body().contains("¿Eliminar la subcategoría?"));
        assertEquals(302, post("/admin/subcategorias/3/eliminar", Map.of(), adminCookie).statusCode());
        assertEquals("DELETE subcategoria 3", ultimaSubcategoria.get());
        assertTrue(get("/admin/categorias?eliminarSub=1", adminCookie).body().contains("No se puede eliminar la subcategoría"));
    }

    @Test
    @DisplayName("Admin depósito: control de acceso por rol")
    void depositoSeguridad() throws Exception {
        assertEquals(302, get("/admin/deposito", null).statusCode());
        assertEquals(403, get("/admin/deposito", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/deposito", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/deposito", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin depósito: flujo de listado, filtros y marcar como vencida")
    void depositoVencerFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        Map<String, Object> d1 = new LinkedHashMap<>(donacion(201, "EN_DEPOSITO", List.of(), "Leche larga vida", "Comedor"));
        Map<String, Object> d2 = new LinkedHashMap<>(donacion(202, "VENCIDA", List.of(), "Fideos", "Comedor"));
        d2.put("justificacion", "Fecha de caducidad superada");
        donacionesAsignadas.add(d1);
        donacionesAsignadas.add(d2);

        // Listar en depósito
        HttpResponse<String> listado = get("/admin/deposito?filtro=en_deposito", adminCookie);
        assertEquals(200, listado.statusCode());
        assertTrue(listado.body().contains("Depósito y Vencimientos"));
        assertTrue(listado.body().contains("#201"));

        // Validar que marcar vencida requiere justificación
        HttpResponse<String> respError = post("/admin/deposito/201/vencer", Map.of("justificacion", ""), adminCookie);
        assertEquals(400, respError.statusCode());
        assertTrue(respError.body().contains("El motivo o justificación es obligatorio"));

        // Marcar como vencida exitosamente
        HttpResponse<String> respVencer = post("/admin/deposito/201/vencer", Map.of("justificacion", "Envase roto y contenido caduco"), adminCookie);
        assertEquals(302, respVencer.statusCode());
        assertEquals("/admin/deposito?filtro=vencidas", respVencer.headers().firstValue("Location").orElse(null));
    }

    @Test
    @DisplayName("Admin matchmaking: control de acceso por rol")
    void matchmakingSeguridad() throws Exception {
        assertEquals(302, get("/admin/matchmaking", null).statusCode());
        assertEquals(403, get("/admin/matchmaking", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/matchmaking", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/matchmaking", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin matchmaking: ejecutar, listar y aceptar o descartar asignación")
    void matchmakingFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // Ejecutar matchmaking
        HttpResponse<String> respEjecutar = post("/admin/matchmaking/ejecutar", Map.of(), adminCookie);
        assertEquals(302, respEjecutar.statusCode());
        assertEquals("/admin/matchmaking", respEjecutar.headers().firstValue("Location").orElse(null));

        // Cargar una propuesta de ejemplo
        propuestasFalsas.add(Map.of(
                "id", 1L,
                "donacionId", 104L,
                "subcategoria", "Leche larga vida",
                "categoria", "Alimentos",
                "bienesResumen", "120 cajas x 1 L",
                "totalUnidades", 120L,
                "entidadesSugeridasIds", List.of(10L, 12L),
                "entidadesSugeridas", List.of(
                        Map.of("id", 10L, "razonSocial", "Comedor Los Bajitos", "direccion", "Av. San Martín 2341", "orden", 1),
                        Map.of("id", 12L, "razonSocial", "Hogar Santa Rita", "direccion", "Belgrano 450", "orden", 2)
                ),
                "fechaEjecucion", "2026-11-03T09:30:00"
        ));

        // Ver listado con la propuesta
        HttpResponse<String> listado = get("/admin/matchmaking", adminCookie);
        assertEquals(200, listado.statusCode());
        assertTrue(listado.body().contains("Matchmaking y Asignaciones"));
        assertTrue(listado.body().contains("Donación #104"));
        assertTrue(listado.body().contains("2 entidades sugeridas"));

        // Abrir modal de sugerencias
        HttpResponse<String> conModal = get("/admin/matchmaking?propuesta=1", adminCookie);
        assertEquals(200, conModal.statusCode());
        assertTrue(conModal.body().contains("Sugerencias de asignación · Propuesta #1"));
        assertTrue(conModal.body().contains("Comedor Los Bajitos"));

        // Aceptar asignación
        HttpResponse<String> respAceptar = post("/admin/matchmaking/propuestas/1/aceptar", Map.of("entidadId", "10"), adminCookie);
        assertEquals(302, respAceptar.statusCode());
        assertEquals("/admin/matchmaking", respAceptar.headers().firstValue("Location").orElse(null));

        // Rechazar propuesta
        HttpResponse<String> respRechazar = post("/admin/matchmaking/propuestas/1/rechazar", Map.of(), adminCookie);
        assertEquals(302, respRechazar.statusCode());
    }

    @Test
    @DisplayName("Admin recepción: control de acceso por rol")
    void recepcionSeguridad() throws Exception {
        assertEquals(302, get("/admin/recepcion", null).statusCode());
        assertEquals(403, get("/admin/recepcion", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/recepcion", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/recepcion", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin recepción: alta rápida de donante y registro de donación en depósito")
    void recepcionFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // Ver pantalla de recepción
        HttpResponse<String> pantalla = get("/admin/recepcion", adminCookie);
        assertEquals(200, pantalla.statusCode());
        assertTrue(pantalla.body().contains("Recepción de Donaciones en Depósito"));

        // Alta rápida de donante inválida (sin DNI)
        Map<String, String> formDonanteInvalido = new LinkedHashMap<>();
        formDonanteInvalido.put("nombre", "Mariano");
        formDonanteInvalido.put("apellido", "López");
        formDonanteInvalido.put("dni", "");
        HttpResponse<String> respErrorDonante = post("/admin/recepcion/donante-humana", formDonanteInvalido, adminCookie);
        assertEquals(400, respErrorDonante.statusCode());
        assertTrue(respErrorDonante.body().contains("El DNI es obligatorio"));

        // Alta rápida de donante válida
        Map<String, String> formDonanteValido = new LinkedHashMap<>(formDonanteInvalido);
        formDonanteValido.put("dni", "34567890");
        formDonanteValido.put("email", "mariano@ejemplo.com");
        formDonanteValido.put("telefono", "1144332211");
        HttpResponse<String> respAltaDonante = post("/admin/recepcion/donante-humana", formDonanteValido, adminCookie);
        assertEquals(302, respAltaDonante.statusCode());
        assertTrue(respAltaDonante.headers().firstValue("Location").orElse("").contains("/admin/recepcion?donanteId="));

        // Alta rápida de persona jurídica válida con todos los datos de empresa que dona
        Map<String, String> formJuridicaValida = new LinkedHashMap<>();
        formJuridicaValida.put("tipoPersona", "JURIDICA");
        formJuridicaValida.put("razonSocial", "Molinos Río de la Plata");
        formJuridicaValida.put("cuit", "30500012349");
        formJuridicaValida.put("rubro", "Alimenticio");
        formJuridicaValida.put("tipo", "EMPRESA");
        formJuridicaValida.put("representanteNombre", "Juan");
        formJuridicaValida.put("representanteApellido", "Pérez");
        formJuridicaValida.put("representanteCorreo", "juan.perez@molinos.com.ar");
        formJuridicaValida.put("email", "contacto@molinos.com.ar");
        formJuridicaValida.put("telefono", "1144332211");
        formJuridicaValida.put("tipoTelefono", "WHATSAPP");
        formJuridicaValida.put("contactoPredeterminado", "EMAIL");
        HttpResponse<String> respAltaJuridica = post("/admin/recepcion/donante", formJuridicaValida, adminCookie);
        assertEquals(302, respAltaJuridica.statusCode());
        assertTrue(respAltaJuridica.headers().firstValue("Location").orElse("").contains("/admin/recepcion?donanteId="));

        // Registro de donación inválido (sin cantidad ni subcategoría)
        Map<String, String> formDonacionInvalido = new LinkedHashMap<>();
        formDonacionInvalido.put("donanteId", "1");
        formDonacionInvalido.put("subcategoria", "");
        formDonacionInvalido.put("bienDescripcion", "Cajas de leche");
        formDonacionInvalido.put("cantidad", "0");
        HttpResponse<String> respErrorDonacion = post("/admin/recepcion", formDonacionInvalido, adminCookie);
        assertEquals(400, respErrorDonacion.statusCode());
        assertTrue(respErrorDonacion.body().contains("La subcategoría es obligatorio"));

        // Registro de donación válido
        Map<String, String> formDonacionValido = new LinkedHashMap<>();
        formDonacionValido.put("donanteId", "1");
        formDonacionValido.put("subcategoria", "Leche larga vida");
        formDonacionValido.put("bienDescripcion", "Cajas de leche entera x 1 L");
        formDonacionValido.put("cantidad", "50");
        formDonacionValido.put("pesoKg", "1.0");
        formDonacionValido.put("volumenM3", "0.02");
        formDonacionValido.put("estadoBien", "NUEVO");
        HttpResponse<String> respAltaDonacion = post("/admin/recepcion", formDonacionValido, adminCookie);
        assertEquals(302, respAltaDonacion.statusCode());
        assertEquals("/admin/deposito", respAltaDonacion.headers().firstValue("Location").orElse(null));

        // Registro con múltiples bienes (para que el backend los segmente automáticamente)
        String multiBody = "donanteId=1&descripcion=Donación mixta"
                + "&subcategoria=Fideos&bienDescripcion=Fideos Matarazzo 500g&cantidad=20&fechaVencimiento=2028-05-01&pesoKg=0.5&volumenM3=0.001&estadoBien=NUEVO"
                + "&subcategoria=Tomate&bienDescripcion=Puré de tomate 520g&cantidad=15&fechaVencimiento=2027-12-01&pesoKg=0.52&volumenM3=0.0008&estadoBien=NUEVO";
        HttpResponse<String> respMulti = postRaw("/admin/recepcion", multiBody, adminCookie);
        assertEquals(302, respMulti.statusCode());
        assertEquals("/admin/deposito", respMulti.headers().firstValue("Location").orElse(null));
    }

    @Test
    @DisplayName("Admin donantes: solo el administrador entra")
    void adminDonantesSeguridad() throws Exception {
        assertEquals(302, get("/admin/donantes", null).statusCode());
        assertEquals(403, get("/admin/donantes", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/donantes", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/donantes", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin donantes: listado vacío con mensaje, en lugar de una tabla vacía")
    void adminDonantesVacio() throws Exception {
        String listado = get("/admin/donantes", cookie(login("admin", "clave-segura"))).body();
        assertTrue(listado.contains("Todavía no hay donantes registrados."));
        assertFalse(listado.contains("<table"));
    }

    @Test
    @DisplayName("Admin donantes: listado con buscador, detalle, edición y baja")
    void adminDonantesFlujo() throws Exception {
        donantesFalsos.add(Map.of("id", 1L, "tipo", "Persona Humana", "nombre", "Ana Gómez", "documento", "30111222",
                "contactoPredeterminado", "ana@mail.com"));
        donantesFalsos.add(Map.of("id", 2L, "tipo", "Persona Jurídica", "nombre", "Molinos S.A.",
                "documento", "30500012349", "contactoPredeterminado", "info@molinos.com"));
        detallesDonantes.put(1L, detalleHumana(1L, 0));
        Map<String, Object> molinos = new HashMap<>(Map.of("id", 2L, "tipoPersona", "JURIDICA",
                "razonSocial", "Molinos S.A.", "cuit", "30500012349", "rubro", "Alimenticio",
                "tipoOrganizacion", "EMPRESA", "cantidadDonaciones", 3, "ultimaDonacion", "2026-10-20"));
        molinos.put("representantes", List.of(Map.of("nombre", "Juan", "apellido", "Pérez",
                "correo", "juan@molinos.com")));
        molinos.put("contactos", List.of(Map.of("tipo", "EMAIL", "valor", "info@molinos.com")));
        molinos.put("contactoPredeterminado", Map.of("tipo", "EMAIL", "valor", "info@molinos.com"));
        detallesDonantes.put(2L, molinos);
        donacionesAsignadas.add(new HashMap<>(Map.of("id", 501L, "estadoActual", "EN_DEPOSITO", "donanteId", 2L,
                "fecha", "2026-10-20T15:30:00", "bienes", List.of())));
        String adminCookie = cookie(login("admin", "clave-segura"));

        // listado: información importante y acciones de cada fila; la cuenta de donaciones sale de /donaciones
        String listado = get("/admin/donantes", adminCookie).body();
        assertTrue(listado.contains("Ana Gómez"));
        assertTrue(listado.contains("30500012349"));
        assertTrue(listado.contains("info@molinos.com"));
        assertTrue(listado.contains("20/10/2026"), "última donación de Molinos");
        assertTrue(listado.contains("/admin/donantes?editar=1"));
        assertTrue(listado.contains("/admin/donantes?eliminar=2"));
        assertTrue(listado.contains("href=\"/admin/donantes\" title=\"Donantes\" aria-current=\"page\""));

        // buscador: por nombre sin tildes y por documento; los links conservan el filtro
        String porNombre = get("/admin/donantes?q=gomez", adminCookie).body();
        assertTrue(porNombre.contains("Ana Gómez"));
        assertFalse(porNombre.contains("Molinos S.A."));
        assertTrue(porNombre.contains("/admin/donantes?editar=1&amp;q=gomez"));
        String porTipo = get("/admin/donantes?tipo=JURIDICA", adminCookie).body();
        assertTrue(porTipo.contains("Molinos S.A."));
        assertFalse(porTipo.contains("Ana Gómez"));
        assertTrue(get("/admin/donantes?q=zzz", adminCookie).body().contains("Ningún donante coincide"));

        // detalle
        String detalle = get("/admin/donantes?ver=2", adminCookie).body();
        assertTrue(detalle.contains("id=\"modal-ver\""));
        assertTrue(detalle.contains("Juan Pérez"));
        assertTrue(detalle.contains("Alimenticio"));

        // edición: el modal abre con los datos cargados y sin poder cambiar el DNI
        String edicion = get("/admin/donantes?editar=1", adminCookie).body();
        assertTrue(edicion.contains("action=\"/admin/donantes/1/editar\""));
        assertTrue(edicion.contains("value=\"Gómez\""));
        assertTrue(edicion.contains("value=\"30111222\" readonly"));
        assertFalse(edicion.contains("id=\"radio-juridica\""), "al editar no se cambia el tipo");

        Map<String, String> invalido = new LinkedHashMap<>();
        invalido.put("nombre", "Ana");
        invalido.put("apellido", "Gómez");
        invalido.put("email", "no-es-email");
        HttpResponse<String> conError = post("/admin/donantes/1/editar", invalido, adminCookie);
        assertEquals(400, conError.statusCode());
        assertTrue(conError.body().contains("Ingresá un email válido"));
        assertTrue(conError.body().contains("value=\"30111222\" readonly"), "el DNI se sigue mostrando");
        assertTrue(conError.body().contains("id=\"modal-donante\" aria-labelledby=\"titulo-donante\" open"));

        Map<String, String> valido = new LinkedHashMap<>(invalido);
        valido.put("email", "ana.gomez@mail.com");
        valido.put("telefono", "1144332211");
        valido.put("tipoTelefono", "SMS");
        valido.put("contactoPredeterminado", "TELEFONO");
        valido.put("volver", "q=gomez");
        HttpResponse<String> editado = post("/admin/donantes/1/editar", valido, adminCookie);
        assertEquals(302, editado.statusCode());
        assertEquals("/admin/donantes?q=gomez", editado.headers().firstValue("Location").orElse(null));
        JsonNode enviado = ultimoDonanteEditado.get();
        assertTrue(enviado.get("dni").isNull(), "el DNI no se manda");
        assertEquals("SMS", enviado.get("contactoPredeterminado").get("tipo").asText());
        assertEquals(3, enviado.get("contactos").size(), "conserva el segundo email que no está en el formulario");

        // un "volver" que no es la consulta del listado no se usa para redirigir
        valido.put("volver", "//otro-sitio.com");
        assertEquals("/admin/donantes", post("/admin/donantes/1/editar", valido, adminCookie)
                .headers().firstValue("Location").orElse(null));

        // baja: con donaciones no se puede; sin donaciones pide confirmación
        String conDonaciones = get("/admin/donantes?eliminar=2", adminCookie).body();
        assertTrue(conDonaciones.contains("No se puede eliminar el donante"));
        assertFalse(conDonaciones.contains("action=\"/admin/donantes/2/eliminar\""));
        String sinDonaciones = get("/admin/donantes?eliminar=1", adminCookie).body();
        assertTrue(sinDonaciones.contains("action=\"/admin/donantes/1/eliminar\""));
        assertTrue(sinDonaciones.contains("Ana Gómez"));
        HttpResponse<String> baja = post("/admin/donantes/1/eliminar", Map.of(), adminCookie);
        assertEquals(302, baja.statusCode());
        assertEquals("1", donanteEliminado.get());

        // alta: con errores vuelve con el modal abierto; bien, va al detalle del nuevo
        HttpResponse<String> altaInvalida = post("/admin/donantes", Map.of("tipoPersona", "HUMANA", "nombre", "Luis"),
                adminCookie);
        assertEquals(400, altaInvalida.statusCode());
        assertTrue(altaInvalida.body().contains("El DNI es obligatorio"));
        assertTrue(altaInvalida.body().contains("value=\"Luis\""));
        Map<String, String> alta = new LinkedHashMap<>();
        alta.put("tipoPersona", "HUMANA");
        alta.put("nombre", "Luis");
        alta.put("apellido", "Paz");
        alta.put("dni", "28999000");
        alta.put("email", "luis@mail.com");
        HttpResponse<String> altaOk = post("/admin/donantes", alta, adminCookie);
        assertEquals(302, altaOk.statusCode());
        assertEquals("/admin/donantes?ver=3", altaOk.headers().firstValue("Location").orElse(null));
    }

    private static Map<String, Object> detalleHumana(long id, int donaciones) {
        Map<String, Object> d = new HashMap<>();
        d.put("id", id);
        d.put("tipoPersona", "HUMANA");
        d.put("nombre", "Ana");
        d.put("apellido", "Gómez");
        d.put("dni", "30111222");
        d.put("fechaNacimiento", "1990-05-04");
        d.put("contactos", List.of(Map.of("tipo", "EMAIL", "valor", "ana@mail.com"),
                Map.of("tipo", "EMAIL", "valor", "ana.trabajo@mail.com")));
        d.put("contactoPredeterminado", Map.of("tipo", "EMAIL", "valor", "ana@mail.com"));
        d.put("representantes", List.of());
        d.put("cantidadDonaciones", donaciones);
        return d;
    }

    @Test
    @DisplayName("Admin importaciones: seguridad de roles")
    void importacionesSeguridad() throws Exception {
        assertEquals(302, get("/admin/importaciones", null).statusCode());
        assertEquals(403, get("/admin/importaciones", cookie(login("comedor", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/importaciones", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(200, get("/admin/importaciones", cookie(login("admin", "clave-segura"))).statusCode());
    }

    @Test
    @DisplayName("Admin importaciones: iniciar importación masiva y consultar estado")
    void importacionesFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // Ver pantalla
        HttpResponse<String> pantalla = get("/admin/importaciones", adminCookie);
        assertEquals(200, pantalla.statusCode());
        assertTrue(pantalla.body().contains("Importación Masiva de Donantes"));
        assertTrue(pantalla.body().contains("Historial de importaciones"));
        assertTrue(pantalla.body().contains("admin"));
        assertFalse(pantalla.body().contains("Registrarme"));

        // Subir archivo CSV
        String csv = "TipoPersona,TipoDoc,Documento,Nombre,Email,Telefono\nHUMANA,,11222333,Ana Lopez,humana@test.com,\n";
        HttpResponse<String> respPost = postArchivo("/admin/importaciones", "archivo", "donantes_2026.csv",
                csv.getBytes(StandardCharsets.UTF_8), adminCookie);
        assertEquals(302, respPost.statusCode());
        assertTrue(respPost.headers().firstValue("Location").orElse("").contains("/admin/importaciones?activa=IMP-105"));

        // Consultar estado JSON de la importación
        HttpResponse<String> estadoJson = get("/admin/importaciones/IMP-105/estado", adminCookie);
        assertEquals(200, estadoJson.statusCode());
        assertTrue(estadoJson.body().contains("IMP-105"));
        assertTrue(estadoJson.body().contains("EN_PROCESO"));
    }

    @Test
    @DisplayName("Admin camiones con cuenta chofer y gestión de rutas de reparto")
    void camionesConCuentaChoferYRutasAdminFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        // Seguridad de /admin/rutas
        assertEquals(302, get("/admin/rutas", null).statusCode());
        assertEquals(403, get("/admin/rutas", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/admin/rutas", cookie(login("chofer", "clave-segura"))).statusCode());

        // Alta de camión configurando cuenta de acceso para el chofer
        Map<String, String> formCamion = new LinkedHashMap<>();
        formCamion.put("patente", "AF 123 CD");
        formCamion.put("capacidadCarga", "4500");
        formCamion.put("capacidadVolumen", "30");
        formCamion.put("altura", "2.8");
        formCamion.put("choferNombre", "Carlos");
        formCamion.put("choferApellido", "Gómez");
        formCamion.put("choferUsername", "chofer1");
        formCamion.put("choferPassword", "demo1234");
        HttpResponse<String> respAlta = post("/admin/camiones", formCamion, adminCookie);
        assertEquals(302, respAlta.statusCode());

        HttpResponse<String> camionesHtml = get("/admin/camiones", adminCookie);
        assertTrue(camionesHtml.body().contains("<code>chofer1</code>"));

        // Ejecutar planificación de rutas y abrir modal de hoja de ruta
        donacionesAsignadas.add(donacion(132, "ASIGNACION_REALIZADA", List.of(), "Frazadas", "Refugio"));
        HttpResponse<String> respPlan = post("/admin/rutas/planificar", Map.of(), adminCookie);
        assertEquals(302, respPlan.statusCode());
        assertEquals("/admin/rutas", respPlan.headers().firstValue("Location").orElse(null));

        HttpResponse<String> rutasHtml = get("/admin/rutas?ver=1", adminCookie);
        assertEquals(200, rutasHtml.statusCode());
        assertTrue(rutasHtml.body().contains("Rutas y entregas"));
        assertTrue(rutasHtml.body().contains("Hoja de ruta #1"));
        assertTrue(rutasHtml.body().contains("Comedor Los Bajitos"));
        assertTrue(rutasHtml.body().contains("<code>chofer1</code>"));
    }

    @Test
    @DisplayName("Chofer: seguridad por rol, hoja de ruta, inicio de recorrido, confirmación y fallo con motivo")
    void choferHojaDeRutaFlujo() throws Exception {
        assertEquals(302, get("/chofer", null).statusCode());
        assertEquals(403, get("/chofer", cookie(login("donante", "clave-segura"))).statusCode());
        assertEquals(403, get("/chofer", cookie(login("entidad", "clave-segura"))).statusCode());
        assertEquals(403, get("/chofer", cookie(login("admin", "clave-segura"))).statusCode());

        String choferCookie = cookie(login("chofer", "clave-segura"));

        // Sin ruta activa muestra tarjeta de estado vacío
        HttpResponse<String> vacia = get("/chofer", choferCookie);
        assertEquals(200, vacia.statusCode());
        assertTrue(vacia.body().contains("No tenés una hoja de ruta activa"));

        // Con una ruta PLANIFICADA de 2 paradas asignada a su camión (id=1)
        donacionesAsignadas.add(donacion(132, "LISTA_PARA_ENTREGAR", List.of(), "Frazadas", "Refugio"));
        rutasFalsas.add(crearRutaFalsa(1L, 1L, "PLANIFICADA"));

        HttpResponse<String> planificada = get("/chofer", choferCookie);
        assertEquals(200, planificada.statusCode());
        assertTrue(planificada.body().contains("<h1>Hoja de ruta</h1>"));
        assertFalse(planificada.body().contains("Mi hoja de ruta"));
        assertTrue(planificada.body().contains("AF 123 CD"));
        assertTrue(planificada.body().contains("Comenzar recorrido"));

        // El mapa toma los datos de atributos data-* (escapados) y un JS aparte: nada de datos dentro del <script>
        assertTrue(planificada.body().contains("id=\"mapa-hoja-ruta\""));
        assertTrue(planificada.body().contains("data-parada data-orden=\"1\""));
        assertTrue(planificada.body().contains("data-lat=\"-34.601200\""));
        assertTrue(planificada.body().contains("src=\"/js/hoja-de-ruta.js\""));
        assertFalse(planificada.body().contains("estado: \""), "el helper estado rompía el JS del mapa");

        // Iniciar recorrido pasa a EN_TRASLADO
        HttpResponse<String> respIniciar = post("/chofer/rutas/1/iniciar", Map.of(), choferCookie);
        assertEquals(302, respIniciar.statusCode());
        assertEquals("/chofer?ruta=1", respIniciar.headers().firstValue("Location").orElse(null));
        assertEquals("EN_TRASLADO", rutasFalsas.get(0).get("estado"));

        // Más de una ruta: el chofer ve todas y elige cuál abrir; por defecto, la que está en traslado
        rutasFalsas.add(crearRutaFalsa(3L, 1L, "PLANIFICADA"));
        rutasFalsas.add(crearRutaFalsa(9L, 2L, "PLANIFICADA")); // de otro camión: no aparece
        String conDos = get("/chofer", choferCookie).body();
        assertTrue(conDos.contains("href=\"/chofer?ruta=1\" aria-current=\"page\""));
        assertTrue(conDos.contains("href=\"/chofer?ruta=3\""));
        assertFalse(conDos.contains("/chofer?ruta=9"));
        assertTrue(conDos.contains("2 rutas por hacer"));
        String rutaTres = get("/chofer?ruta=3", choferCookie).body();
        assertTrue(rutaTres.contains("href=\"/chofer?ruta=3\" aria-current=\"page\""));
        assertTrue(rutaTres.contains("Estás haciendo la"), "no se empieza otra ruta con una en traslado");
        assertFalse(rutaTres.contains("action=\"/chofer/rutas/3/iniciar\""));
        assertEquals(302, post("/chofer/rutas/3/iniciar", Map.of(), choferCookie).statusCode());
        assertEquals("PLANIFICADA", rutasFalsas.get(1).get("estado"));
        assertTrue(get("/chofer?ruta=9", choferCookie).body().contains("href=\"/chofer?ruta=1\" aria-current=\"page\""),
                "una ruta de otro camión no se abre");

        // Abrir modal de confirmación de la parada 11 y confirmarla
        HttpResponse<String> modalConfirmar = get("/chofer?ruta=1&confirmarParada=11", choferCookie);
        assertTrue(modalConfirmar.body().contains("Confirmar entrega en Parada #1"));
        assertFalse(get("/chofer?ruta=3&confirmarParada=11", choferCookie).body().contains("modal-confirmar-entrega"),
                "una ruta planificada no confirma paradas");
        HttpResponse<String> respConfirmar = post("/chofer/rutas/1/paradas/11/confirmar", Map.of(), choferCookie);
        assertEquals(302, respConfirmar.statusCode());
        assertEquals("/chofer?ruta=1", respConfirmar.headers().firstValue("Location").orElse(null));

        // Reportar fallo sin motivo devuelve 400 con el modal abierto
        HttpResponse<String> falloSinMotivo = post("/chofer/rutas/1/paradas/12/fallar", Map.of("motivo", ""), choferCookie);
        assertEquals(400, falloSinMotivo.statusCode());
        assertTrue(falloSinMotivo.body().contains("id=\"modal-fallar-entrega\""));
        assertTrue(falloSinMotivo.body().contains("El motivo es obligatorio"));

        // Reportar fallo con motivo en la última parada finaliza la ruta y redirige al modal de ruta completada
        HttpResponse<String> falloOk = post("/chofer/rutas/1/paradas/12/fallar",
                Map.of("motivo", "Comedor cerrado sin referente"), choferCookie);
        assertEquals(302, falloOk.statusCode());
        assertEquals("/chofer?completada=1", falloOk.headers().firstValue("Location").orElse(null));
        assertEquals("Comedor cerrado sin referente", ultimoMotivoFallo.get());

        HttpResponse<String> modalCompletadaMixta = get("/chofer?completada=1", choferCookie);
        assertEquals(200, modalCompletadaMixta.statusCode());
        assertTrue(modalCompletadaMixta.body().contains("id=\"modal-ruta-completada\""));
        assertTrue(modalCompletadaMixta.body().contains("¡Ruta #1 completada!"));
        assertTrue(modalCompletadaMixta.body().contains("1 entregada · 1 no recibida"));
        assertTrue(modalCompletadaMixta.body().contains("Te queda 1 ruta por hacer."));
        assertTrue(modalCompletadaMixta.body().contains("Ir a la próxima ruta"));

        // Segunda ruta con todas las paradas entregadas muestra el modal de todas las paradas completadas
        rutasFalsas.add(crearRutaFalsa(2L, 1L, "EN_TRASLADO"));
        assertEquals(302, post("/chofer/rutas/2/paradas/11/confirmar", Map.of(), choferCookie).statusCode());
        HttpResponse<String> finRuta2 = post("/chofer/rutas/2/paradas/12/confirmar", Map.of(), choferCookie);
        assertEquals(302, finRuta2.statusCode());
        assertEquals("/chofer?completada=2", finRuta2.headers().firstValue("Location").orElse(null));

        HttpResponse<String> modalTodasEntregadas = get("/chofer?completada=2", choferCookie);
        assertEquals(200, modalTodasEntregadas.statusCode());
        assertTrue(modalTodasEntregadas.body().contains("id=\"modal-ruta-completada\""));
        assertTrue(modalTodasEntregadas.body().contains("¡Ruta #2 completada!"));
        assertTrue(modalTodasEntregadas.body().contains("Completaste todas las paradas del recorrido asignado a tu camión."));
        assertTrue(modalTodasEntregadas.body().contains("2 paradas resueltas (2 entregadas)"));
    }

    @Test
    @DisplayName("Admin depósito: pestaña de entregas fallidas permite reprogramar envío o volver a depósito")
    void depositoEntregasFallidasFlujo() throws Exception {
        String adminCookie = cookie(login("admin", "clave-segura"));

        Map<String, Object> fallida = new LinkedHashMap<>(
                donacion(305, "ENTREGA_FALLIDA", List.of(), "Frazadas", "Refugio"));
        fallida.put("entidadAsignadaId", 10L);
        fallida.put("justificacion", "Comedor cerrado sin referente");
        donacionesAsignadas.add(fallida);

        HttpResponse<String> pestanaFallidas = get("/admin/deposito?filtro=fallidas", adminCookie);
        assertEquals(200, pestanaFallidas.statusCode());
        assertTrue(pestanaFallidas.body().contains("#305"));
        assertTrue(pestanaFallidas.body().contains("Comedor Los Bajitos"));
        assertTrue(pestanaFallidas.body().contains("Comedor cerrado sin referente"));
        assertTrue(pestanaFallidas.body().contains("Reprogramar envío"));
        assertTrue(pestanaFallidas.body().contains("Volver a depósito"));

        // Reprogramar pasa a ASIGNACION_REALIZADA
        HttpResponse<String> respReprog = post("/admin/deposito/305/reprogramar", Map.of(), adminCookie);
        assertEquals(302, respReprog.statusCode());
        assertEquals("ASIGNACION_REALIZADA", fallida.get("estadoActual"));

        // Si vuelve a fallar, reingresar la devuelve a EN_DEPOSITO
        fallida.put("estadoActual", "ENTREGA_FALLIDA");
        HttpResponse<String> respReing = post("/admin/deposito/305/reingresar", Map.of(), adminCookie);
        assertEquals(302, respReing.statusCode());
        assertEquals("/admin/deposito?filtro=en_deposito", respReing.headers().firstValue("Location").orElse(null));
        assertEquals("EN_DEPOSITO", fallida.get("estadoActual"));
    }

    // ---- API falsa ----

    private Javalin crearApiFalsa() {
        Javalin api = Javalin.create(config -> config.showJavalinBanner = false);

        api.post("/api/auth/login", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            String username = body.get("username").asText();
            if (!"clave-segura".equals(body.get("password").asText())) {
                ctx.status(401).json(Map.of("error", "unauthorized", "message", "Credenciales invalidas"));
                return;
            }
            String rol = switch (username) {
                case "entidad" -> "ENTIDAD_BENEFICIARIA";
                case "admin" -> "ADMINISTRADOR";
                case "chofer" -> "CHOFER";
                default -> "DONANTE";
            };
            String refresh = "vencido".equals(username) ? "refresh-malo" : "refresh-ok";
            ctx.json(sesion(username, rol, "access-viejo", refresh));
        });

        api.get("/api/auth/choferes", ctx -> ctx.json(choferesFalsos));
        api.post("/api/auth/chofer", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            long camionId = body.get("camionId").asLong();
            String username = body.get("username").asText();
            choferesFalsos.removeIf(u -> ((Number) u.get("perfilId")).longValue() == camionId);
            Map<String, Object> creado = Map.of(
                    "usuarioId", choferesFalsos.size() + 20L,
                    "username", username,
                    "rol", "CHOFER",
                    "perfilId", camionId);
            choferesFalsos.add(creado);
            ctx.status(200).json(creado);
        });

        api.post("/api/auth/refresh", ctx -> {
            if (!"refresh-ok".equals(mapper.readTree(ctx.body()).get("refreshToken").asText())) {
                ctx.status(401).json(Map.of("error", "unauthorized", "message", "Refresh token invalido"));
                return;
            }
            ctx.json(Map.of("accessToken", "access-nuevo", "refreshToken", "refresh-ok"));
        });

        api.get("/api/auth/me", ctx -> {
            if (!"Bearer access-nuevo".equals(ctx.header("Authorization"))) {
                ctx.status(401).json(Map.of("error", "unauthorized", "message", "Token vencido"));
                return;
            }
            ctx.json(Map.of("usuarioId", 1, "username", "donante", "rol", "DONANTE", "perfilId", 10));
        });

        // la API verifica la direccion con Georef: una inventada se rechaza con un mensaje sobre la direccion
        api.post("/api/auth/registro/entidad-beneficiaria", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            String direccion = body.get("entidad").get("direccion").asText();
            if (direccion.contains("Inventada")) {
                ctx.status(400).json(Map.of("error", "bad_request", "message",
                        "La dirección \"" + direccion + "\" no existe o no la encontramos."));
                return;
            }
            ultimoRegistro.set(body);
            ctx.status(201).json(sesion(body.get("username").asText(), "ENTIDAD_BENEFICIARIA", "access-viejo", "refresh-ok"));
        });

        // la API ubica la direccion con Georef; aca solo se conoce Av. Rivadavia 6500
        api.get("/api/direcciones/ubicar", ctx -> {
            String direccion = ctx.queryParam("direccion");
            if (direccion == null || !direccion.startsWith("Av. Rivadavia 6500, Flores, ")) {
                ctx.status(400).json(Map.of("error", "bad_request", "message",
                        "La dirección \"" + direccion + "\" no existe o no la encontramos."));
                return;
            }
            ctx.json(Map.of("latitud", -34.6271, "longitud", -58.4578,
                    "normalizada", "AV RIVADAVIA 6500, Comuna 7, Ciudad Autónoma de Buenos Aires"));
        });

        api.post("/api/auth/registro/persona-humana", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimoRegistro.set(body);
            String username = body.get("username").asText();
            if ("repetido".equals(username)) {
                ctx.status(400).json(Map.of("error", "bad_request",
                        "message", "El nombre de usuario repetido ya esta en uso"));
                return;
            }
            ctx.status(201).json(sesion(username, "DONANTE", "access-viejo", "refresh-ok"));
        });

        api.get("/api/categorias", ctx -> ctx.json(List.of(
                Map.of("id", 1, "nombre", "Alimentos"), Map.of("id", 2, "nombre", "Ropa y abrigo"))));
        api.get("/api/categorias/1/subcategorias", ctx -> ctx.json(List.of(
                Map.of("id", 1, "nombre", "Leche larga vida"), Map.of("id", 2, "nombre", "Arroz"))));
        api.get("/api/categorias/2/subcategorias", ctx -> ctx.json(List.of(Map.of("id", 3, "nombre", "Frazadas"))));
        api.get("/api/subcategorias", ctx -> ctx.json(List.of(
                Map.of("id", 1, "nombre", "Leche larga vida", "categoriaId", 1, "categoria", "Alimentos"),
                Map.of("id", 2, "nombre", "Arroz", "categoriaId", 1, "categoria", "Alimentos"),
                Map.of("id", 3, "nombre", "Frazadas", "categoriaId", 2, "categoria", "Ropa y abrigo"))));
        // ABM de categorias: guarda lo ultimo que mando el web y rechaza nombres repetidos
        api.post("/api/categorias", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimaCategoria.set(body);
            if ("Alimentos".equals(body.get("nombre").asText())) {
                ctx.status(400).json(Map.of("error", "bad_request",
                        "message", "Ya existe una categoría con el nombre 'Alimentos'"));
                return;
            }
            ctx.status(201).json(Map.of("id", 3, "nombre", body.get("nombre").asText(),
                    "pideEstado", body.get("pideEstado").asBoolean(), "esPerecedero", body.get("esPerecedero").asBoolean()));
        });
        api.put("/api/categorias/{id}", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimaCategoria.set(body);
            ctx.json(Map.of("id", Long.parseLong(ctx.pathParam("id")), "nombre", body.get("nombre").asText(),
                    "pideEstado", body.get("pideEstado").asBoolean(), "esPerecedero", body.get("esPerecedero").asBoolean()));
        });
        api.post("/api/categorias/{id}/subcategorias", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimaSubcategoria.set("POST " + ctx.pathParam("id") + " " + body.get("nombre").asText());
            ctx.status(201).json(Map.of("id", 4, "nombre", body.get("nombre").asText(), "categoriaId", 1, "categoria", "Alimentos"));
        });
        // Bajas: "Alimentos" (1) y su "Leche larga vida" (1) estan en uso; "Ropa y abrigo" (2) y "Frazadas" (3), no
        api.get("/api/categorias/{id}/usos", ctx -> ctx.json("1".equals(ctx.pathParam("id"))
                ? Map.of("donaciones", 3, "necesidades", 1) : Map.of("donaciones", 0, "necesidades", 0)));
        api.get("/api/subcategorias/{id}/usos", ctx -> ctx.json("1".equals(ctx.pathParam("id"))
                ? Map.of("donaciones", 3, "necesidades", 1) : Map.of("donaciones", 0, "necesidades", 0)));
        api.delete("/api/categorias/{id}", ctx -> {
            if ("1".equals(ctx.pathParam("id"))) {
                ctx.status(400).json(Map.of("error", "bad_request",
                        "message", "No se puede eliminar la categoría 'Alimentos': la usan 3 donaciones y 1 necesidad"));
                return;
            }
            ultimaSubcategoria.set("DELETE categoria " + ctx.pathParam("id"));
            ctx.status(204);
        });
        api.delete("/api/subcategorias/{id}", ctx -> {
            ultimaSubcategoria.set("DELETE subcategoria " + ctx.pathParam("id"));
            ctx.status(204);
        });
        api.put("/api/subcategorias/{id}", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimaSubcategoria.set("PUT " + ctx.pathParam("id") + " " + body.get("nombre").asText());
            ctx.json(Map.of("id", 2, "nombre", body.get("nombre").asText(), "categoriaId", 1, "categoria", "Alimentos"));
        });

        api.get("/api/entidad-beneficiaria", ctx -> ctx.json(List.of(
                Map.of("id", 10, "razonSocial", "Comedor Los Bajitos", "direccion", "Av. San Martín 2341"),
                Map.of("id", 12, "razonSocial", "Hogar Santa Rita", "direccion", "Belgrano 450"))));
        api.get("/api/entidad-beneficiaria/10/necesidades", ctx -> ctx.json(necesidades));
        api.post("/api/entidad-beneficiaria/10/necesidades", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimaNecesidad.set(body);
            if ("Inexistente".equals(body.get("subcategoria").get("nombre").asText())) {
                ctx.status(404).json(Map.of("error", "not_found",
                        "message", "No existe la subcategoria 'Inexistente' en el catalogo"));
                return;
            }
            Map<String, Object> necesidad = new LinkedHashMap<>();
            necesidad.put("id", necesidades.size() + 1);
            necesidad.put("subcategoria", body.get("subcategoria").get("nombre").asText());
            necesidad.put("categoria", "Ropa y abrigo");
            necesidad.put("tipoNecesidad", "NecesidadRecurrente");
            necesidad.put("periodo", body.get("periodo").asText());
            necesidad.put("descripcion", body.get("descripcion").asText());
            necesidad.put("cantidad", body.get("cantidad").asLong());
            necesidad.put("unidad", body.get("unidad").asText());
            necesidad.put("cantidadAsignada", 12);
            necesidad.put("satisfecha", false);
            necesidades.add(necesidad);
            ctx.status(201).json(necesidad);
        });
        api.delete("/api/entidad-beneficiaria/10/necesidades/{id}", ctx -> {
            necesidadEliminada.set(ctx.pathParam("id"));
            if ("7".equals(ctx.pathParam("id"))) {
                ctx.status(400).json(Map.of("error", "bad_request", "message",
                        "No se puede eliminar la necesidad: tiene 1 donación asignada que todavía no llegó"));
                return;
            }
            ctx.status(204);
        });

        api.get("/api/donaciones", ctx -> {
            filtroDonaciones.set(ctx.queryParam("entidadId"));
            String estado = ctx.queryParam("estado");
            if (estado != null && !estado.isBlank()) {
                ctx.json(donacionesAsignadas.stream()
                        .filter(d -> estado.equalsIgnoreCase((String) d.get("estadoActual")))
                        .toList());
            } else {
                ctx.json(donacionesAsignadas);
            }
        });
        api.put("/api/donaciones/{id}/estado", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            JsonNode body = mapper.readTree(ctx.body());
            String nuevoEstado = body.get("estado").asText();
            String justificacion = body.has("justificacion") ? body.get("justificacion").asText() : null;
            donacionesAsignadas.stream()
                    .filter(d -> ((Number) d.get("id")).longValue() == id)
                    .findFirst()
                    .ifPresent(d -> {
                        d.put("estadoActual", nuevoEstado);
                        d.put("justificacion", justificacion);
                    });
            ctx.json(Map.of("id", id, "estadoActual", nuevoEstado));
        });
        api.get("/api/donaciones/entregadas", ctx -> ctx.json(entregas));
        api.get("/api/donaciones/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            donacionesAsignadas.stream().filter(d -> ((Number) d.get("id")).longValue() == id).findFirst()
                    .ifPresentOrElse(ctx::json, () -> ctx.status(404).json(Map.of("error", "not_found",
                            "message", "No se encontro una donacion con el id: " + id)));
        });
        api.post("/api/donaciones/{id}/recepcion", ctx -> {
            ultimaRecepcion.set(mapper.readTree(ctx.body()));
            if ("122".equals(ctx.pathParam("id"))) {
                ctx.status(400).json(Map.of("error", "bad_request",
                        "message", "La recepción de esta donación ya fue confirmada"));
                return;
            }
            ctx.json(Map.of("id", Long.parseLong(ctx.pathParam("id"))));
        });

        // Camiones
        List<Map<String, Object>> camionesFalsos = new ArrayList<>();
        api.get("/api/camiones", ctx -> ctx.json(camionesFalsos));
        api.get("/api/camiones/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(Map.of("id", id, "patente", "AF 123 CD", "capacidadVolumen", 30.0, "altura", 2.8, "capacidadCarga", 4500.0,
                    "chofer", Map.of("nombre", "Carlos", "apellido", "Gómez")));
        });
        api.post("/api/camiones", ctx -> {
            Map<String, Object> nuevo = Map.of("id", 1L, "patente", "AF 123 CD", "capacidadVolumen", 30.0,
                    "altura", 2.8, "capacidadCarga", 4500.0,
                    "chofer", Map.of("nombre", "Carlos", "apellido", "Gómez"));
            camionesFalsos.removeIf(c -> ((Number) c.get("id")).longValue() == 1L);
            camionesFalsos.add(nuevo);
            ctx.status(201).json(nuevo);
        });
        api.put("/api/camiones/{id}", ctx -> {
            ctx.json(Map.of("id", Long.parseLong(ctx.pathParam("id")), "patente", "AF 123 CD", "capacidadVolumen", 35.0, "altura", 3.0, "capacidadCarga", 5000.0,
                    "chofer", Map.of("nombre", "Carlos", "apellido", "Gómez")));
        });
        api.delete("/api/camiones/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            camionesFalsos.removeIf(c -> ((Number) c.get("id")).longValue() == id);
            ctx.status(204);
        });

        // Rutas de logística
        api.get("/api/rutas", ctx -> ctx.json(rutasFalsas));
        api.post("/api/rutas/planificaciones", ctx -> {
            if (!donacionesAsignadas.isEmpty()) {
                rutasFalsas.add(crearRutaFalsa(rutasFalsas.size() + 1L, 1L, "PLANIFICADA"));
            }
            ctx.status(204);
        });
        api.get("/api/rutas/camion/{id}", ctx -> {
            long camionId = Long.parseLong(ctx.pathParam("id"));
            ctx.json(rutasFalsas.stream()
                    .filter(r -> ((Number) r.get("camionId")).longValue() == camionId)
                    .toList());
        });
        api.get("/api/rutas/camion/{id}/activa", ctx -> {
            long camionId = Long.parseLong(ctx.pathParam("id"));
            rutasFalsas.stream()
                    .filter(r -> ((Number) r.get("camionId")).longValue() == camionId)
                    .filter(r -> !"FINALIZADA".equals(r.get("estado")))
                    .findFirst()
                    .ifPresentOrElse(ctx::json, () -> ctx.status(204));
        });
        api.get("/api/rutas/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            rutasFalsas.stream()
                    .filter(r -> ((Number) r.get("id")).longValue() == id)
                    .findFirst()
                    .ifPresentOrElse(ctx::json, () -> ctx.status(404).json(Map.of("message", "No existe la ruta")));
        });
        api.put("/api/rutas/{id}/estado", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            String nuevoEstado = ctx.body().replace("\"", "").trim();
            rutasFalsas.stream()
                    .filter(r -> ((Number) r.get("id")).longValue() == id)
                    .findFirst()
                    .ifPresent(r -> r.put("estado", nuevoEstado));
            ctx.status(204);
        });
        api.post("/api/rutas/{rutaId}/paradas/{paradaId}/confirmaciones", ctx -> {
            actualizarEstadoParadaFalsa(Long.parseLong(ctx.pathParam("rutaId")),
                    Long.parseLong(ctx.pathParam("paradaId")), "ENTREGADA");
            ctx.status(204);
        });
        api.post("/api/rutas/{rutaId}/paradas/{paradaId}/fallos", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            ultimoMotivoFallo.set(body.has("motivo") ? body.get("motivo").asText() : null);
            actualizarEstadoParadaFalsa(Long.parseLong(ctx.pathParam("rutaId")),
                    Long.parseLong(ctx.pathParam("paradaId")), "NO_RECIBIDA");
            ctx.status(204);
        });

        // Matchmaking
        api.get("/api/matchmaking/pendientes", ctx -> ctx.json(propuestasFalsas));
        api.post("/api/matchmaking/ejecuciones", ctx -> ctx.status(204));
        api.put("/api/matchmaking/propuestas/{id}/estado", ctx -> ctx.status(204));

        // Donantes y recepción presencial
        api.get("/api/donantes", ctx -> ctx.json(donantesFalsos));
        api.post("/api/donantes/persona-humana", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            long id = donantesFalsos.size() + 1L;
            Map<String, Object> donante = Map.of(
                    "id", id,
                    "tipo", "Persona Humana",
                    "nombre", body.get("nombre").asText() + " " + body.get("apellido").asText(),
                    "documento", body.get("dni").asText()
            );
            donantesFalsos.add(donante);
            ctx.status(201).json(donante);
        });
        api.post("/api/donantes/persona-juridica", ctx -> {
            JsonNode body = mapper.readTree(ctx.body());
            long id = donantesFalsos.size() + 1L;
            Map<String, Object> donante = Map.of(
                    "id", id,
                    "tipo", "Persona Jurídica",
                    "nombre", body.get("razonSocial").asText(),
                    "documento", body.get("cuit").asText()
            );
            donantesFalsos.add(donante);
            ctx.status(201).json(donante);
        });
        api.post("/api/donaciones", ctx -> {
            ctx.status(201).json(List.of(Map.of("id", 401L, "estadoActual", "EN_DEPOSITO")));
        });
        // ABM de donantes del administrador
        api.get("/api/donantes/{id}/detalle", ctx -> {
            Map<String, Object> detalle = detallesDonantes.get(Long.parseLong(ctx.pathParam("id")));
            if (detalle == null) {
                ctx.status(404).json(Map.of("error", "not_found", "message", "No se encontro un donante con el id: "
                        + ctx.pathParam("id")));
                return;
            }
            ctx.json(detalle);
        });
        api.put("/api/donantes/persona-humana/{id}", ctx -> {
            ultimoDonanteEditado.set(mapper.readTree(ctx.body()));
            ctx.json(Map.of("id", Long.parseLong(ctx.pathParam("id")), "tipo", "Persona Humana",
                    "nombre", ultimoDonanteEditado.get().get("nombre").asText()));
        });
        api.delete("/api/donantes/{id}", ctx -> {
            donanteEliminado.set(ctx.pathParam("id"));
            ctx.status(204);
        });

        List<Map<String, Object>> importacionesFalsas = new ArrayList<>(List.of(
                importacionFake("IMP-104", "padron_donantes_caba.csv", "COMPLETADA", 12500, 12500, 11890, 610, 0, 100)
        ));

        api.get("/api/donantes/importaciones", ctx -> ctx.json(importacionesFalsas));
        api.get("/api/donantes/importaciones/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Map<String, Object> imp = importacionesFalsas.stream()
                    .filter(i -> id.equals(i.get("id")))
                    .findFirst()
                    .orElse(importacionesFalsas.get(0));
            ctx.json(imp);
        });
        api.post("/api/donantes/importaciones", ctx -> {
            String nombre = ctx.header("X-Filename") != null ? ctx.header("X-Filename") : "donantes.csv";
            Map<String, Object> nueva = importacionFake("IMP-105", nombre, "EN_PROCESO", 10000, 7200, 6850, 310, 40, 72);
            nueva.put("errores", List.of(Map.of("fila", 142, "valor", "ana.lopez@invalid", "motivo", "Documento vacío")));
            importacionesFalsas.add(0, nueva);
            ctx.status(202).json(nueva);
        });

        return api;
    }

    private Map<String, Object> importacionFake(String id, String nombreArchivo, String estado, int total, int proc, int creados, int act, int desc, int pct) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("nombreArchivo", nombreArchivo);
        map.put("fecha", "03/11/2026 10:15");
        map.put("estado", estado);
        map.put("totalFilas", total);
        map.put("procesadas", proc);
        map.put("creados", creados);
        map.put("actualizados", act);
        map.put("descartadas", desc);
        map.put("porcentaje", pct);
        map.put("errores", List.of());
        return map;
    }

    /** Una donacion como la devuelve GET /donaciones?entidadId=10 (necesidad null = se elimino). */
    private Map<String, Object> donacion(long id, String estado, List<String> fotos, String subcategoria,
            String descripcionNecesidad) {
        Map<String, Object> donacion = new LinkedHashMap<>();
        donacion.put("id", id);
        donacion.put("bienes", List.of(Map.of("descripcion", "Arroz largo fino", "cantidad", 60)));
        donacion.put("estadoActual", estado);
        donacion.put("subcategoria", "Útiles escolares");
        donacion.put("fechaAsignacion", "2026-10-29T10:00:00");
        donacion.put("fechaEstado", "2026-11-01T15:30:00");
        donacion.put("necesidad", subcategoria == null ? null
                : Map.of("id", 1, "subcategoria", subcategoria, "descripcion", descripcionNecesidad));
        donacion.put("fotosRecepcion", fotos);
        return donacion;
    }

    private Map<String, Object> entidadConEntregas(long id, String razonSocial, Double latitud, Double longitud,
            List<Map<String, Object>> deLaEntidad) {
        Map<String, Object> entidad = new LinkedHashMap<>();
        entidad.put("entidadId", id);
        entidad.put("razonSocial", razonSocial);
        entidad.put("direccion", "San Martín 1234, CABA");
        entidad.put("latitud", latitud); // nulas: la API todavia no la pudo geocodificar
        entidad.put("longitud", longitud);
        entidad.put("entregas", deLaEntidad);
        return entidad;
    }

    private Map<String, Object> entrega(long donacionId, String bien, long cantidad, List<String> fotos) {
        return Map.of("donacionId", donacionId, "subcategoria", "Arroz", "categoria", "Alimentos",
                "fechaEntrega", "2026-11-01T15:30:00",
                "bienes", List.of(Map.of("descripcion", bien, "cantidad", cantidad)), "fotos", fotos);
    }

    private Map<String, Object> sesion(String username, String rol, String access, String refresh) {
        long perfilId = "CHOFER".equals(rol) ? 1L : 10L;
        return Map.of("accessToken", access, "refreshToken", refresh,
                "usuario", Map.of("usuarioId", 1, "username", username, "rol", rol, "perfilId", perfilId));
    }

    private Map<String, Object> crearRutaFalsa(long rutaId, long camionId, String estado) {
        Map<String, Object> p1 = new LinkedHashMap<>();
        p1.put("id", 11L);
        p1.put("orden", 1);
        p1.put("destino", "Av. San Martín 2341, Paternal, CABA");
        p1.put("entidad", 10L);
        p1.put("entregas", List.of(132L));
        p1.put("estado", "PENDIENTE");
        p1.put("latitud", -34.6012);
        p1.put("longitud", -58.4682);

        Map<String, Object> p2 = new LinkedHashMap<>();
        p2.put("id", 12L);
        p2.put("orden", 2);
        p2.put("destino", "Belgrano 450, Balvanera, CABA");
        p2.put("entidad", 12L);
        p2.put("entregas", List.of(132L));
        p2.put("estado", "PENDIENTE");
        p2.put("latitud", -34.6128);
        p2.put("longitud", -58.3731);

        Map<String, Object> ruta = new LinkedHashMap<>();
        ruta.put("id", rutaId);
        ruta.put("fecha", "2026-11-06");
        ruta.put("estado", estado);
        ruta.put("patenteCamion", "AF 123 CD");
        ruta.put("paradas", new ArrayList<>(List.of(p1, p2)));
        ruta.put("choferNombre", "Carlos");
        ruta.put("choferApellido", "Gómez");
        ruta.put("camionId", camionId);
        ruta.put("camionLatitud", -34.5986);
        ruta.put("camionLongitud", -58.4202);
        return ruta;
    }

    @SuppressWarnings("unchecked")
    private void actualizarEstadoParadaFalsa(long rutaId, long paradaId, String nuevoEstado) {
        rutasFalsas.stream()
                .filter(r -> ((Number) r.get("id")).longValue() == rutaId)
                .findFirst()
                .ifPresent(r -> {
                    List<Map<String, Object>> paradas = (List<Map<String, Object>>) r.get("paradas");
                    paradas.stream()
                            .filter(p -> ((Number) p.get("id")).longValue() == paradaId)
                            .findFirst()
                            .ifPresent(p -> p.put("estado", nuevoEstado));
                    boolean todasResueltas = paradas.stream()
                            .noneMatch(p -> "PENDIENTE".equals(p.get("estado")));
                    if (todasResueltas) {
                        r.put("estado", "FINALIZADA");
                    }
                });
    }

    // ---- "Navegador" ----

    private Map<String, String> formularioPersona(String username) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("nombre", "Ana");
        form.put("apellido", "Peña");
        form.put("dni", "30111222");
        form.put("fechaNacimiento", "1990-05-04");
        form.put("genero", "Femenino");
        form.put("direccion", "Av. Medrano 951");
        form.put("email", "ana@mail.com");
        form.put("telefono", "1155556666");
        form.put("tipoTelefono", "WHATSAPP");
        form.put("contactoPredeterminado", "TELEFONO");
        form.put("username", username);
        form.put("password", "clave-segura");
        form.put("password2", "clave-segura");
        return form;
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return post("/login", Map.of("username", username, "password", password), null);
    }

    /** "donatrack_session=..." de la respuesta, para mandarlo en el pedido siguiente. */
    private String cookie(HttpResponse<String> respuesta) {
        return respuesta.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("donatrack_session="))
                .map(c -> c.split(";")[0])
                .reduce((primera, ultima) -> ultima)
                .orElseThrow(() -> new AssertionError("La respuesta no trae la cookie de sesion"));
    }

    private HttpResponse<String> get(String ruta, String cookie) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(WEB + ruta)).GET();
        if (cookie != null) {
            pedido.header("Cookie", cookie);
        }
        return navegador.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postRaw(String ruta, String body, String cookie) throws Exception {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(WEB + ruta))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            pedido.header("Cookie", cookie);
        }
        return navegador.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String ruta, Map<String, String> campos, String cookie) throws Exception {
        String body = campos.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(WEB + ruta))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            pedido.header("Cookie", cookie);
        }
        return navegador.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Un formulario multipart/form-data con archivos en el campo "fotos" (nombre -> contenido). */
    private HttpResponse<String> postFotos(String ruta, Map<String, byte[]> fotos, String cookie) throws Exception {
        String limite = "----donatrack" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (fotos.isEmpty()) {
            // lo que manda el navegador con el input vacio
            fotos = Map.of("", new byte[0]);
        }
        for (Map.Entry<String, byte[]> foto : fotos.entrySet()) {
            body.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"fotos\"; filename=\""
                    + foto.getKey() + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write(foto.getValue());
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(WEB + ruta))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return navegador.send(pedido, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postArchivo(String ruta, String campo, String nombreArchivo, byte[] contenido, String cookie) throws Exception {
        String limite = "----donatrack" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + campo + "\"; filename=\""
                + nombreArchivo + "\"\r\nContent-Type: text/csv\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(contenido);
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(WEB + ruta))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return navegador.send(pedido, HttpResponse.BodyHandlers.ofString());
    }

    /** Los primeros bytes de un PNG: alcanza para que AlmacenFotos lo reconozca. */
    private static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    }
}
