package ar.edu.utn.frba.ddsi.notificaciones;

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
@DisplayName("Autenticación y autorización en la API de notificaciones")
class NotificacionesAuthTest {

    private static final int PUERTO = 18084;
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
    @DisplayName("GET /api/notificaciones: 401 sin token, 403 para donante accediendo a otro donante, 200 para propietario")
    void seguridadNotificaciones() throws Exception {
        // 1. Sin token -> 401
        HttpRequest reqSinToken = HttpRequest.newBuilder(URI.create(BASE + "/notificaciones")).GET().build();
        HttpResponse<String> resSinToken = http.send(reqSinToken, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resSinToken.statusCode(), "Debe responder 401 Unauthorized sin credenciales");

        // 2. Token de donante 1 pidiendo notificaciones de donante 2 -> 403
        String tokenDonante1 = jwtUtil.generarAccessToken(new UsuarioAutenticado(10L, "juan.donante", Rol.DONANTE, 1L));
        HttpRequest reqOtro = HttpRequest.newBuilder(URI.create(BASE + "/notificaciones?destinatarioId=2&tipoDestinatario=DONANTE"))
                .header("Authorization", "Bearer " + tokenDonante1)
                .GET().build();
        HttpResponse<String> resOtro = http.send(reqOtro, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resOtro.statusCode(), "Debe responder 403 Forbidden para donante que no es propietario");

        // 3. Token de donante 1 pidiendo sus propias notificaciones -> 200
        HttpRequest reqPropio = HttpRequest.newBuilder(URI.create(BASE + "/notificaciones?destinatarioId=1&tipoDestinatario=DONANTE"))
                .header("Authorization", "Bearer " + tokenDonante1)
                .GET().build();
        HttpResponse<String> resPropio = http.send(reqPropio, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resPropio.statusCode(), "Debe responder 200 OK para el propietario");

        // 4. Clave de servicio (rol SISTEMA) -> 200
        HttpRequest reqSistema = HttpRequest.newBuilder(URI.create(BASE + "/notificaciones"))
                .header(AutenticacionHandler.HEADER_API_KEY, serviceApiKey)
                .GET().build();
        HttpResponse<String> resSistema = http.send(reqSistema, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resSistema.statusCode(), "Debe responder 200 OK para rol SISTEMA con clave compartida");
    }
}

