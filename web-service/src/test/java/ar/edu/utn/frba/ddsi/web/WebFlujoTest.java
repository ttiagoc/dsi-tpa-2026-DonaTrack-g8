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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.web.clients.ApiClient;
import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.config.WebConfig;
import ar.edu.utn.frba.ddsi.web.controllers.HomeController;
import ar.edu.utn.frba.ddsi.web.controllers.PanelController;
import ar.edu.utn.frba.ddsi.web.controllers.RegistroController;
import ar.edu.utn.frba.ddsi.web.controllers.SesionController;
import ar.edu.utn.frba.ddsi.web.exceptions.NoEncontradoException;
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

    private Javalin apiFalsa;
    private Javalin web;

    @BeforeEach
    void levantar() {
        apiFalsa = crearApiFalsa().start(PUERTO_API);

        WebConfig config = new WebConfig();
        config.getSesion().setMinutosInactividad(30);
        AuthApiClient auth = new AuthApiClient("http://localhost:" + PUERTO_API + "/api",
                HttpClient.newHttpClient(), ApiClient.crearObjectMapper());
        Router router = new Router(new AutorizacionMiddleware(), new HomeController(), new SesionController(auth),
                new RegistroController(auth), new PanelController());

        web = new Server(router, config).crear();
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
    @DisplayName("Registro de persona: muestra el error de negocio que devuelve la API")
    void registroErrorDeNegocio() throws Exception {
        HttpResponse<String> respuesta = post("/registro/persona-humana", formularioPersona("repetido"), null);
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("El nombre de usuario repetido ya esta en uso"));
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
                default -> "DONANTE";
            };
            String refresh = "vencido".equals(username) ? "refresh-malo" : "refresh-ok";
            ctx.json(sesion(username, rol, "access-viejo", refresh));
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

        return api;
    }

    private Map<String, Object> sesion(String username, String rol, String access, String refresh) {
        return Map.of("accessToken", access, "refreshToken", refresh,
                "usuario", Map.of("usuarioId", 1, "username", username, "rol", rol, "perfilId", 10));
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
}
