package ar.edu.utn.frba.ddsi.logistica;

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
import ar.edu.utn.frba.ddsi.common.security.AutenticacionHandler;
import ar.edu.utn.frba.ddsi.common.security.JwtUtil;
import ar.edu.utn.frba.ddsi.common.security.Rol;
import ar.edu.utn.frba.ddsi.common.security.UsuarioAutenticado;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Autenticación y autorización en la API de logística")
class LogisticaAuthTest {

    private static final int PUERTO = 18083;
    private static final String BASE = "http://localhost:" + PUERTO + "/api";
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    private List<JavalinController> controllers;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${seguridad.service-api-key:donatrack-clave-servicios-local}")
    private String serviceApiKey;

    private JavalinWebServer servidor;

    @BeforeEach
    void iniciarServidor() {
        servidor = new JavalinWebServer(controllers);
        ReflectionTestUtils.setField(servidor, "port", PUERTO);
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop();
    }

    @Test
    @DisplayName("Acceso a /api/camiones: 401 sin token, 403 para donante, 200 para admin y sistema")
    void seguridadCamiones() throws Exception {
        // 1. Sin token -> 401
        HttpRequest reqSinToken = HttpRequest.newBuilder(URI.create(BASE + "/camiones")).GET().build();
        HttpResponse<String> resSinToken = http.send(reqSinToken, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resSinToken.statusCode(), "Debe responder 401 Unauthorized sin credenciales");

        // 2. Con token de DONANTE -> 403
        String tokenDonante = jwtUtil.generarAccessToken(new UsuarioAutenticado(10L, "juan.donante", Rol.DONANTE, 1L));
        HttpRequest reqDonante = HttpRequest.newBuilder(URI.create(BASE + "/camiones"))
                .header("Authorization", "Bearer " + tokenDonante)
                .GET().build();
        HttpResponse<String> resDonante = http.send(reqDonante, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resDonante.statusCode(), "Debe responder 403 Forbidden para rol DONANTE");

        // 3. Con token de ADMINISTRADOR -> 200
        String tokenAdmin = jwtUtil.generarAccessToken(new UsuarioAutenticado(1L, "admin", Rol.ADMINISTRADOR, null));
        HttpRequest reqAdmin = HttpRequest.newBuilder(URI.create(BASE + "/camiones"))
                .header("Authorization", "Bearer " + tokenAdmin)
                .GET().build();
        HttpResponse<String> resAdmin = http.send(reqAdmin, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resAdmin.statusCode(), "Debe responder 200 OK para rol ADMINISTRADOR");

        // 4. Con header X-Api-Key de servicio (rol SISTEMA) -> 200
        HttpRequest reqSistema = HttpRequest.newBuilder(URI.create(BASE + "/camiones"))
                .header(AutenticacionHandler.HEADER_API_KEY, serviceApiKey)
                .GET().build();
        HttpResponse<String> resSistema = http.send(reqSistema, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resSistema.statusCode(), "Debe responder 200 OK para rol SISTEMA con API Key válida");
    }

    @Test
    @DisplayName("Planificación de rutas: 401 sin token, 403 para donante, 204 para admin y sistema")
    void seguridadPlanificacionRutas() throws Exception {
        // 1. Sin token -> 401
        HttpRequest reqSinToken = HttpRequest.newBuilder(URI.create(BASE + "/rutas/planificaciones"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resSinToken = http.send(reqSinToken, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resSinToken.statusCode());

        // 2. Con token de DONANTE -> 403
        String tokenDonante = jwtUtil.generarAccessToken(new UsuarioAutenticado(10L, "juan.donante", Rol.DONANTE, 1L));
        HttpRequest reqDonante = HttpRequest.newBuilder(URI.create(BASE + "/rutas/planificaciones"))
                .header("Authorization", "Bearer " + tokenDonante)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resDonante = http.send(reqDonante, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resDonante.statusCode());

        // 3. Con token de ADMINISTRADOR -> 204
        String tokenAdmin = jwtUtil.generarAccessToken(new UsuarioAutenticado(1L, "admin", Rol.ADMINISTRADOR, null));
        HttpRequest reqAdmin = HttpRequest.newBuilder(URI.create(BASE + "/rutas/planificaciones"))
                .header("Authorization", "Bearer " + tokenAdmin)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resAdmin = http.send(reqAdmin, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, resAdmin.statusCode());

        // 4. Con header X-Api-Key -> 204
        HttpRequest reqSistema = HttpRequest.newBuilder(URI.create(BASE + "/rutas/planificaciones"))
                .header(AutenticacionHandler.HEADER_API_KEY, serviceApiKey)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resSistema = http.send(reqSistema, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, resSistema.statusCode());
    }
}
