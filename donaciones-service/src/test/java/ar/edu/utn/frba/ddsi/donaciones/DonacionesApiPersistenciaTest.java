package ar.edu.utn.frba.ddsi.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.controllers.JavalinWebServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Recorre la API real (Javalin + servicios + JPA sobre HSQLDB) para verificar que cada request
 * lee lo que guardaron los anteriores, aun liberando el EntityManager al final de cada uno.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("API de donaciones persistiendo con JPA")
class DonacionesApiPersistenciaTest {

    private static final int PUERTO = 18080;
    private static final String BASE = "http://localhost:" + PUERTO + "/api";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private List<JavalinController> controllers;

    private JavalinWebServer servidor;

    // JavalinWebServer no se levanta con el perfil "test": se inicia a mano con los controllers del contexto
    @BeforeEach
    void levantarServidor() {
        servidor = new JavalinWebServer(controllers);
        ReflectionTestUtils.setField(servidor, "port", PUERTO);
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop();
    }

    @Test
    @DisplayName("Carga catalogo, donante, donacion y entidad, y ejecuta el matchmaking")
    void flujoCompleto() throws Exception {
        String sufijo = String.valueOf(System.nanoTime());

        JsonNode categoria = post("/categorias",
                "{\"nombre\":\"Alimentos " + sufijo + "\",\"pideEstado\":false,\"esPerecedero\":true}", 201);
        String subcategoria = "Fideos " + sufijo;
        post("/categorias/" + categoria.get("id").asLong() + "/subcategorias",
                "{\"nombre\":\"" + subcategoria + "\"}", 201);

        JsonNode donante = post("/donantes/persona-humana", """
                {"nombre":"Ana","apellido":"Perez","fechaNacimiento":"1990-05-01","dni":"12345678",
                 "genero":"F","direccion":"Medrano 951",
                 "contactos":[{"tipo":"EMAIL","valor":"ana%s@mail.com"}],
                 "contactoPredeterminado":{"tipo":"EMAIL","valor":"ana%s@mail.com"}}
                """.formatted(sufijo, sufijo), 201);

        JsonNode donaciones = post("/donaciones", """
                {"descripcion":"Donacion de fideos","idDonante":%d,
                 "bienes":[{"descripcion":"Fideos secos","cantidad":100,"pesoKgPorUnidad":0.5,
                            "volumenM3PorUnidad":0.001,"estado":"NUEVO","fechaVencimiento":"2027-01-01",
                            "subcategoria":{"nombre":"%s"}}]}
                """.formatted(donante.get("id").asLong(), subcategoria), 201);
        assertEquals(1, donaciones.size());
        long idDonacion = donaciones.get(0).get("id").asLong();

        JsonNode leida = get("/donaciones/" + idDonacion);
        assertEquals("EN_DEPOSITO", leida.get("estadoActual").asText());
        assertEquals(donante.get("id").asLong(), leida.get("donanteId").asLong());

        JsonNode entidad = post("/entidad-beneficiaria", """
                {"razonSocial":"Comedor %s","direccion":"Escobar 123","telefono":"1122334455",
                 "correoRepresentantes":[{"tipo":"EMAIL","valor":"comedor@org.com"}]}
                """.formatted(sufijo), 201);
        long idEntidad = entidad.get("id").asLong();
        post("/entidad-beneficiaria/" + idEntidad + "/necesidades", """
                {"subcategoria":{"nombre":"%s"},"tipoNecesidad":"recurrente","periodo":"SEMANAL",
                 "descripcion":"Fideos para el comedor","cantidad":50}
                """.formatted(subcategoria), 201);

        JsonNode necesidades = get("/entidad-beneficiaria/" + idEntidad + "/necesidades");
        assertEquals("SEMANAL", necesidades.get(0).get("periodo").asText());

        enviar(HttpRequest.newBuilder(URI.create(BASE + "/matchmaking/ejecuciones"))
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        JsonNode pendientes = get("/matchmaking/pendientes");
        JsonNode propuesta = null;
        for (JsonNode p : pendientes) {
            if (p.get("donacionId").asLong() == idDonacion) {
                propuesta = p;
            }
        }
        assertTrue(propuesta != null, "El matchmaking debe generar una propuesta para la donacion");

        HttpResponse<String> aceptar = enviar(HttpRequest.newBuilder(
                URI.create(BASE + "/matchmaking/propuestas/" + propuesta.get("id").asLong() + "/estado"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        "{\"estado\":\"ACEPTADO\",\"entidadId\":" + idEntidad + "}"))
                .build());
        assertTrue(aceptar.statusCode() < 300, "Aceptar la propuesta devolvio " + aceptar.body());

        assertEquals("ASIGNACION_REALIZADA", get("/donaciones/" + idDonacion).get("estadoActual").asText());
    }

    private JsonNode post(String path, String json, int statusEsperado) throws Exception {
        HttpResponse<String> response = enviar(HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
        assertEquals(statusEsperado, response.statusCode(), "POST " + path + " -> " + response.body());
        return mapper.readTree(response.body());
    }

    private JsonNode get(String path) throws Exception {
        HttpResponse<String> response = enviar(HttpRequest.newBuilder(URI.create(BASE + path)).GET().build());
        assertEquals(200, response.statusCode(), "GET " + path + " -> " + response.body());
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> enviar(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
