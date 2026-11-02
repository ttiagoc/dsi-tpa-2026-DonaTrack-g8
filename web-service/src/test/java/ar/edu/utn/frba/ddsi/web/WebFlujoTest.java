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
import ar.edu.utn.frba.ddsi.web.config.WebConfig;
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
        Router router = new Router(new AutorizacionMiddleware(), new HomeController(), new SesionController(auth),
                new RegistroController(auth), new PanelController(), new NecesidadesController(donaciones),
                new DonacionesAsignadasController(donaciones), new RecepcionController(donaciones, fotos),
                new MapaController(donaciones));

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
        assertTrue(panel.contains("Panel de la entidad"), "titulo de la barra");
        assertTrue(panel.contains("Entidad beneficiaria"), "rol en el perfil");
        assertTrue(panel.contains("href=\"/entidad\" title=\"Inicio\" aria-current=\"page\""), "marca la pagina abierta");
        assertTrue(panel.contains("for=\"menu-plegado\""), "boton para achicar el menu");
        assertFalse(panel.contains("Notificaciones"), "la campana no es de este plan");
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
                "frecuencia", "SEMANAL", "cantidad", "30", "descripcion", "Para el refugio"), cookie);
        assertEquals(302, alta.statusCode(), alta.body());
        assertEquals("/entidad/necesidades", alta.headers().firstValue("Location").orElse(null));

        JsonNode enviado = ultimaNecesidad.get();
        assertEquals("Frazadas", enviado.get("subcategoria").get("nombre").asText());
        assertEquals("recurrente", enviado.get("tipoNecesidad").asText());
        assertEquals("SEMANAL", enviado.get("periodo").asText());
        assertEquals(30, enviado.get("cantidad").asLong());

        String listado = get("/entidad/necesidades", cookie).body();
        assertTrue(listado.contains("Necesidad agregada."));
        assertTrue(listado.contains("Todas las semanas"));
        assertTrue(listado.contains("12 de 30"));
        assertTrue(listado.contains("width: 40%"), "barra de progreso");
        assertTrue(listado.contains("Pendiente"));
    }

    @Test
    @DisplayName("Necesidades: con datos invalidos vuelve con el modal abierto, los errores y lo cargado")
    void necesidadInvalida() throws Exception {
        HttpResponse<String> respuesta = post("/entidad/necesidades", Map.of("subcategoria", "Arroz",
                "frecuencia", "", "cantidad", "0", "descripcion", "Almuerzos"), cookie(login("entidad", "clave-segura")));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("id=\"modal-alta\" aria-labelledby=\"titulo-alta\" open"));
        assertTrue(respuesta.body().contains("Elegí cada cuánto la necesitan"));
        assertTrue(respuesta.body().contains("Tiene que ser un número mayor a 0"));
        assertTrue(respuesta.body().contains("<option selected>Arroz</option>"), "conserva la subcategoria");
        assertTrue(respuesta.body().contains(">Almuerzos</textarea>"), "conserva la descripcion");
        assertEquals(null, ultimaNecesidad.get(), "no deberia haber llamado a la API");
    }

    @Test
    @DisplayName("Necesidades: el error de la API se muestra dentro del modal")
    void necesidadErrorDeLaApi() throws Exception {
        HttpResponse<String> respuesta = post("/entidad/necesidades", Map.of("subcategoria", "Inexistente",
                "frecuencia", "UNA_VEZ", "cantidad", "5", "descripcion", "x"), cookie(login("entidad", "clave-segura")));
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
        entregas.add(entidadConEntregas(6, "Comedor San Martín", List.of(
                entrega(2, "Arroz largo fino 1 kg", 60, List.of("/fotos/a.png", "/fotos/b.png")),
                entrega(3, "Leche entera 1 L", 12, List.of()))));
        entregas.add(entidadConEntregas(9, "Escuela Rural 10", List.of(
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
        assertTrue(pagina.contains("Mapa ilustrativo"));
    }

    @Test
    @DisplayName("Mapa: con sesion se ve con el menu lateral, que tiene el link al mapa")
    void mapaConSesion() throws Exception {
        String pagina = get("/mapa", cookie(login("entidad", "clave-segura"))).body();
        assertTrue(pagina.contains("class=\"menu-lateral\""));
        assertTrue(pagina.contains("<a href=\"/mapa\">Mapa de entregas</a>"));
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

        api.get("/api/categorias", ctx -> ctx.json(List.of(
                Map.of("id", 1, "nombre", "Alimentos"), Map.of("id", 2, "nombre", "Ropa y abrigo"))));
        api.get("/api/categorias/1/subcategorias", ctx -> ctx.json(List.of(
                Map.of("id", 1, "nombre", "Leche larga vida"), Map.of("id", 2, "nombre", "Arroz"))));
        api.get("/api/categorias/2/subcategorias", ctx -> ctx.json(List.of(Map.of("id", 3, "nombre", "Frazadas"))));

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
            ctx.json(donacionesAsignadas);
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

        return api;
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

    private Map<String, Object> entidadConEntregas(long id, String razonSocial, List<Map<String, Object>> deLaEntidad) {
        return Map.of("entidadId", id, "razonSocial", razonSocial, "direccion", "San Martín 1234",
                "entregas", deLaEntidad);
    }

    private Map<String, Object> entrega(long donacionId, String bien, long cantidad, List<String> fotos) {
        return Map.of("donacionId", donacionId, "subcategoria", "Arroz", "categoria", "Alimentos",
                "fechaEntrega", "2026-11-01T15:30:00",
                "bienes", List.of(Map.of("descripcion", bien, "cantidad", cantidad)), "fotos", fotos);
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

    /** Los primeros bytes de un PNG: alcanza para que AlmacenFotos lo reconozca. */
    private static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    }
}
