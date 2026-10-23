package ar.edu.utn.frba.ddsi.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.controllers.JavalinWebServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Recorre la API real para verificar la autenticacion: registro y login, 401 sin credenciales,
 * 403 con el rol equivocado o sobre recursos ajenos, refresh y la clave de servicio.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Autenticacion y autorizacion de la API de donaciones")
class AuthApiTest {

    private static final int PUERTO = 18081;
    private static final String BASE = "http://localhost:" + PUERTO + "/api";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private List<JavalinController> controllers;

    @Value("${seguridad.service-api-key}")
    private String serviceApiKey;

    private JavalinWebServer servidor;
    private String sufijo;

    @BeforeEach
    void levantarServidor() {
        servidor = new JavalinWebServer(controllers);
        ReflectionTestUtils.setField(servidor, "port", PUERTO);
        servidor.start();
        sufijo = String.valueOf(System.nanoTime());
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop();
    }

    @Test
    @DisplayName("Un donante se registra, queda logueado y puede volver a entrar")
    void registroYLogin() throws Exception {
        JsonNode sesion = registrarDonante("ana" + sufijo);
        assertEquals("DONANTE", sesion.get("usuario").get("rol").asText());

        HttpResponse<String> me = enviar(get("/auth/me").header("Authorization", bearer(sesion)));
        assertEquals(200, me.statusCode(), me.body());
        assertEquals("ana" + sufijo, mapper.readTree(me.body()).get("username").asText());

        assertEquals(200, login("ANA" + sufijo, "clave-segura").statusCode());
        assertEquals(401, login("ana" + sufijo, "incorrecta").statusCode());
        assertEquals(401, login("nadie" + sufijo, "clave-segura").statusCode());
    }

    @Test
    @DisplayName("Registrar un username repetido devuelve 400")
    void usernameRepetido() throws Exception {
        registrarDonante("repetido" + sufijo);

        HttpResponse<String> otra = enviar(post("/auth/registro/persona-humana",
                cuerpoRegistroDonante("Repetido" + sufijo, "otro" + sufijo)));

        assertEquals(400, otra.statusCode(), otra.body());
    }

    @Test
    @DisplayName("Sin credenciales: las rutas publicas responden y las protegidas devuelven 401")
    void sinCredenciales() throws Exception {
        assertEquals(200, enviar(get("/categorias")).statusCode());
        assertEquals(401, enviar(get("/donantes")).statusCode());
        assertEquals(401, enviar(get("/donaciones/1")).statusCode());
        assertEquals(401, enviar(get("/donantes").header("Authorization", "Bearer no-es-un-token")).statusCode());
    }

    @Test
    @DisplayName("Un donante no accede a rutas de administrador ni a datos de otro donante")
    void donanteConRolEquivocado() throws Exception {
        JsonNode ana = registrarDonante("ana" + sufijo);
        JsonNode beto = registrarDonante("beto" + sufijo);
        long idAna = ana.get("usuario").get("perfilId").asLong();
        long idBeto = beto.get("usuario").get("perfilId").asLong();

        assertEquals(403, enviar(get("/donantes").header("Authorization", bearer(ana))).statusCode());
        assertEquals(403, enviar(get("/matchmaking/pendientes").header("Authorization", bearer(ana))).statusCode());
        assertEquals(200, enviar(get("/donantes/" + idAna).header("Authorization", bearer(ana))).statusCode());
        assertEquals(403, enviar(get("/donantes/" + idBeto).header("Authorization", bearer(ana))).statusCode());

        // no puede registrar una donacion a nombre de otro donante
        HttpResponse<String> ajena = enviar(post("/donaciones",
                "{\"descripcion\":\"x\",\"idDonante\":" + idBeto + ",\"bienes\":[]}")
                .header("Authorization", bearer(ana)));
        assertEquals(403, ajena.statusCode(), ajena.body());
    }

    @Test
    @DisplayName("Una entidad gestiona sus necesidades pero no las de otra entidad")
    void entidadSoloLoSuyo() throws Exception {
        JsonNode comedor = registrarEntidad("comedor" + sufijo);
        JsonNode escuela = registrarEntidad("escuela" + sufijo);
        long idComedor = comedor.get("usuario").get("perfilId").asLong();
        long idEscuela = escuela.get("usuario").get("perfilId").asLong();

        assertEquals("ENTIDAD_BENEFICIARIA", comedor.get("usuario").get("rol").asText());
        assertEquals(200, enviar(get("/entidad-beneficiaria/" + idComedor + "/necesidades")
                .header("Authorization", bearer(comedor))).statusCode());
        assertEquals(403, enviar(get("/entidad-beneficiaria/" + idEscuela + "/necesidades")
                .header("Authorization", bearer(comedor))).statusCode());
    }

    @Test
    @DisplayName("El refresh token renueva el access token y no sirve como access token")
    void refresh() throws Exception {
        JsonNode sesion = registrarDonante("ana" + sufijo);
        String refreshToken = sesion.get("refreshToken").asText();

        HttpResponse<String> renovado = enviar(post("/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}"));
        assertEquals(200, renovado.statusCode(), renovado.body());
        String nuevoAccess = mapper.readTree(renovado.body()).get("accessToken").asText();

        assertEquals(200, enviar(get("/auth/me").header("Authorization", "Bearer " + nuevoAccess)).statusCode());
        assertEquals(401, enviar(get("/auth/me").header("Authorization", "Bearer " + refreshToken)).statusCode());
    }

    @Test
    @DisplayName("La clave de servicio da acceso a las rutas de SISTEMA y una clave incorrecta devuelve 401")
    void claveDeServicio() throws Exception {
        long idComedor = registrarEntidad("comedor" + sufijo).get("usuario").get("perfilId").asLong();
        String contactos = "/entidad-beneficiaria/" + idComedor + "/contactos";

        assertEquals(200, enviar(get(contactos).header("X-Api-Key", serviceApiKey)).statusCode());
        assertEquals(401, enviar(get(contactos).header("X-Api-Key", "incorrecta")).statusCode());
        // SISTEMA no tiene acceso a las rutas exclusivas del administrador
        assertEquals(403, enviar(get("/donantes").header("X-Api-Key", serviceApiKey)).statusCode());
    }

    private JsonNode registrarDonante(String username) throws Exception {
        HttpResponse<String> response = enviar(post("/auth/registro/persona-humana",
                cuerpoRegistroDonante(username, username)));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    private String cuerpoRegistroDonante(String username, String email) {
        return """
                {"username":"%s","password":"clave-segura",
                 "donante":{"nombre":"Ana","apellido":"Perez","fechaNacimiento":"1990-05-01","dni":"12345678",
                            "genero":"F","direccion":"Medrano 951",
                            "contactos":[{"tipo":"EMAIL","valor":"%s@mail.com"}],
                            "contactoPredeterminado":{"tipo":"EMAIL","valor":"%s@mail.com"}}}
                """.formatted(username, email, email);
    }

    private JsonNode registrarEntidad(String username) throws Exception {
        HttpResponse<String> response = enviar(post("/auth/registro/entidad-beneficiaria", """
                {"username":"%s","password":"clave-segura",
                 "entidad":{"razonSocial":"Entidad %s","direccion":"Escobar 123","telefono":"1122334455",
                            "correoRepresentantes":[{"tipo":"EMAIL","valor":"%s@org.com"}]}}
                """.formatted(username, username, username)));
        assertEquals(201, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return enviar(post("/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private String bearer(JsonNode sesion) {
        return "Bearer " + sesion.get("accessToken").asText();
    }

    private HttpRequest.Builder get(String path) {
        return HttpRequest.newBuilder(URI.create(BASE + path)).GET();
    }

    private HttpRequest.Builder post(String path, String json) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpResponse<String> enviar(HttpRequest.Builder request) throws Exception {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
