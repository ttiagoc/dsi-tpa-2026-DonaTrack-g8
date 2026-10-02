package ar.edu.utn.frba.ddsi.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("E2E Test: Creacion y Verificacion de Objetos Reales en PostgreSQL (TPA 1, 2 y 3)")
public class PostgresE2EVerificationTest {

    private static final String DONACIONES_URL = "http://localhost:8080/api";
    private static final String LOGISTICA_URL = "http://localhost:8081/api";
    private static final String NOTIFICACIONES_URL = "http://localhost:8082/api";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Crea objetos reales en PostgreSQL a traves de los microservicios y valida su persistencia")
    void testCreacionRealEnPostgresYFlujoConsignas() throws Exception {
        Assumptions.assumeTrue(serviciosDisponibles(),
                "Los microservicios y PostgreSQL deben estar corriendo en localhost para ejecutar este test E2E");

        String timestamp = String.valueOf(System.currentTimeMillis());

        System.out.println("\n====================================================================");
        System.out.println(">>> INICIANDO TEST E2E: CREACIÓN DE OBJETOS REALES EN POSTGRESQL <<<");
        System.out.println("====================================================================\n");

        // -------------------------------------------------------------------------
        // 1. TPA 1: Crear Categorías y Subcategorías
        // -------------------------------------------------------------------------
        System.out.println("[Paso 1] Creando Catalogo (Categorias y Subcategorias)...");
        JsonNode catAlimentos = post(DONACIONES_URL + "/categorias", """
                {"nombre":"Alimentos %s","pideEstado":false,"esPerecedero":true}
                """.formatted(timestamp), 201);
        long catId = catAlimentos.get("id").asLong();

        String subcatNombre = "Fideos Secos " + timestamp;
        post(DONACIONES_URL + "/categorias/" + catId + "/subcategorias", """
                {"nombre":"%s"}
                """.formatted(subcatNombre), 201);

        // -------------------------------------------------------------------------
        // 2. TPA 1: Crear Donante Humano y Donante Jurídico
        // -------------------------------------------------------------------------
        System.out.println("[Paso 2] Registrando Donantes (Humano y Juridico)...");
        JsonNode donanteHumano = post(DONACIONES_URL + "/donantes/persona-humana", """
                {"nombre":"Ana","apellido":"Perez","fechaNacimiento":"1992-04-15","dni":"%s",
                 "genero":"F","direccion":"Av. Medrano 951",
                 "contactos":[{"tipo":"EMAIL","valor":"ana%s@mail.com"},{"tipo":"WHATSAPP","valor":"+541155555555"}],
                 "contactoPredeterminado":{"tipo":"EMAIL","valor":"ana%s@mail.com"}}
                """.formatted(timestamp.substring(Math.max(0, timestamp.length() - 8)), timestamp, timestamp), 201);
        long donanteId = donanteHumano.get("id").asLong();

        post(DONACIONES_URL + "/donantes/persona-juridica", """
                {"razonSocial":"Arcos Plateados %s S.A.","rubro":"Gastronomia","tipo":"EMPRESA",
                 "cuit":"30-%s-9",
                 "representantes":[{"nombre":"Carlos","apellido":"Gomez","correo":"rep%s@arcos.com"}],
                 "contactos":[{"tipo":"EMAIL","valor":"contacto%s@empresa.com"}],
                 "contactoPredeterminado":{"tipo":"EMAIL","valor":"contacto%s@empresa.com"}}
                """.formatted(timestamp, timestamp.substring(Math.max(0, timestamp.length() - 8)), timestamp, timestamp, timestamp), 201);

        // -------------------------------------------------------------------------
        // 3. TPA 1: Registrar Donación y Segmentación Automática
        // -------------------------------------------------------------------------
        System.out.println("[Paso 3] Registrando Donacion con bienes perecederos (Segmentacion automatica)...");
        JsonNode donacionesCreadas = post(DONACIONES_URL + "/donaciones", """
                {"descripcion":"Colecta comunitaria","idDonante":%d,
                 "bienes":[{"descripcion":"Paquetes de Fideos 500g","cantidad":100,"pesoKgPorUnidad":0.5,
                            "volumenM3PorUnidad":0.001,"estado":"NUEVO","fechaVencimiento":"2027-01-01",
                            "subcategoria":{"nombre":"%s"}}]}
                """.formatted(donanteId, subcatNombre), 201);
        assertTrue(donacionesCreadas.isArray() && donacionesCreadas.size() > 0);
        long donacionId = donacionesCreadas.get(0).get("id").asLong();

        JsonNode donacionLeida = get(DONACIONES_URL + "/donaciones/" + donacionId);
        assertEquals("EN_DEPOSITO", donacionLeida.get("estadoActual").asText());

        // -------------------------------------------------------------------------
        // 4. TPA 1: Crear Entidad Beneficiaria y Necesidades
        // -------------------------------------------------------------------------
        System.out.println("[Paso 4] Registrando Entidad Beneficiaria y Necesidades...");
        JsonNode entidad = post(DONACIONES_URL + "/entidad-beneficiaria", """
                {"razonSocial":"Comedor Escobar Sonrisas %s","direccion":"Calle Falsa 123, Escobar",
                 "telefono":"1122334455",
                 "correoRepresentantes":[{"tipo":"EMAIL","valor":"comedor%s@org.com"}]}
                """.formatted(timestamp, timestamp), 201);
        long entidadId = entidad.get("id").asLong();

        post(DONACIONES_URL + "/entidad-beneficiaria/" + entidadId + "/necesidades", """
                {"subcategoria":{"nombre":"%s"},"tipoNecesidad":"recurrente","periodo":"SEMANAL",
                 "descripcion":"Fideos para viandas comunitarias","cantidad":80}
                """.formatted(subcatNombre), 201);

        // -------------------------------------------------------------------------
        // 5. TPA 2: Matchmaking y Asignación de Donación
        // -------------------------------------------------------------------------
        System.out.println("[Paso 5] Ejecutando algoritmo de Matchmaking y asignando destino...");
        enviarVacio(DONACIONES_URL + "/matchmaking/ejecuciones");

        JsonNode propuestas = get(DONACIONES_URL + "/matchmaking/pendientes");
        JsonNode miPropuesta = null;
        for (JsonNode p : propuestas) {
            if (p.get("donacionId").asLong() == donacionId) {
                miPropuesta = p;
                break;
            }
        }
        assertNotNull(miPropuesta, "El matchmaking debio generar una propuesta para la donacion");
        long propuestaId = miPropuesta.get("id").asLong();

        put(DONACIONES_URL + "/matchmaking/propuestas/" + propuestaId + "/estado", """
                {"estado":"ACEPTADO","entidadId":%d}
                """.formatted(entidadId), 204);

        JsonNode donacionAsignada = get(DONACIONES_URL + "/donaciones/" + donacionId);
        assertEquals("ASIGNACION_REALIZADA", donacionAsignada.get("estadoActual").asText());

        // -------------------------------------------------------------------------
        // 6. TPA 2: Logística (Camión, Ruta, Parada, Monitoreo GPS y Entrega)
        // -------------------------------------------------------------------------
        System.out.println("[Paso 6] Gestion de Logistica (Camion, Ruta, Telemetria GPS)...");
        String patente = "AB" + (int)(Math.random() * 900 + 100) + "CD";
        JsonNode camion = post(LOGISTICA_URL + "/camiones", """
                {"patente":"%s","capacidadVolumen":30.0,"altura":3.8,"capacidadCarga":8000.0,
                 "chofer":{"nombre":"Juan","apellido":"Perez"}}
                """.formatted(patente), 201);
        long camionId = camion.get("id").asLong();

        JsonNode ruta = post(LOGISTICA_URL + "/rutas", """
                {"fecha":"2026-10-02","idCamion":%d,
                 "paradas":[{"orden":1,"destino":"Calle Falsa 123, Escobar","entidad":%d,"entregas":[%d]}]}
                """.formatted(camionId, entidadId, donacionId), 201);
        long rutaId = ruta.get("id").asLong();
        long paradaId = ruta.get("paradas").get(0).get("id").asLong();

        // Chofer inicia la ruta -> pasa a EN_TRASLADO
        put(LOGISTICA_URL + "/rutas/" + rutaId + "/estado", "\"EN_TRASLADO\"", 204);

        // Telemetría GPS en tiempo real
        post(LOGISTICA_URL + "/camiones/ubicacion/" + patente, """
                {"latitud":-34.6037,"longitud":-58.3816,"velocidad":45.5}
                """, 204);

        // Confirmar entrega de la parada
        post(LOGISTICA_URL + "/rutas/" + rutaId + "/paradas/" + paradaId + "/confirmaciones", "", 204);

        // -------------------------------------------------------------------------
        // 7. TPA 1 & TPA 2: Notificaciones (Persistencia del evento en esquema notificaciones)
        // -------------------------------------------------------------------------
        System.out.println("[Paso 7] Registrando Notificacion del evento...");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/notificaciones", "postgres", "postgres");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO notificacion (mensaje, contacto_tipo_contacto, contacto_valor, completada, fecha_de_envio, tipo_evento, donacion_id, destinatario_id) " +
                    "VALUES ('¡Tu donacion de Fideos fue entregada exitosamente!', 'EMAIL', 'ana" + timestamp + "@mail.com', true, NOW(), 'ENTREGA_EXITOSA', " + donacionId + ", " + donanteId + ")");
        }

        // -------------------------------------------------------------------------
        // 8. TPA 3: Verificación Directa con Consultas SQL contra PostgreSQL Real
        // -------------------------------------------------------------------------
        System.out.println("\n[Paso 8] VERIFICANDO PERSISTENCIA DIRECTA EN POSTGRESQL (docker localhost:5432)...");

        // Esquema 'donaciones'
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/donaciones", "postgres", "postgres");
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM donacion WHERE id = " + donacionId);
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "La donacion debe existir fisicamente en la tabla donacion de PostgreSQL");

            rs = stmt.executeQuery("SELECT count(*) FROM donante WHERE id = " + donanteId);
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "El donante debe existir fisicamente en la tabla donante de PostgreSQL");

            rs = stmt.executeQuery("SELECT count(*) FROM entidad_beneficiaria WHERE id = " + entidadId);
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "La entidad beneficiaria debe existir fisicamente en PostgreSQL");

            rs = stmt.executeQuery("SELECT estado, count(*) FROM cambio_estado WHERE donacion_id = " + donacionId + " GROUP BY estado");
            System.out.println("  -> Estados registrados en PostgreSQL para la donacion " + donacionId + ":");
            while (rs.next()) {
                System.out.println("     * Estado: " + rs.getString(1) + " (count: " + rs.getInt(2) + ")");
            }
        }

        // Esquema 'logistica'
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/logistica", "postgres", "postgres");
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM camion WHERE patente = '" + patente + "'");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "El camion debe existir fisicamente en la tabla camion de PostgreSQL");

            rs = stmt.executeQuery("SELECT count(*) FROM ruta WHERE id = " + rutaId);
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "La ruta debe existir fisicamente en la tabla ruta de PostgreSQL");
        }

        // Esquema 'notificaciones'
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/notificaciones", "postgres", "postgres");
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM notificacion WHERE contacto_valor = 'ana" + timestamp + "@mail.com'");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "La notificacion debe existir fisicamente en la tabla notificacion de PostgreSQL");
        }

        System.out.println("\n====================================================================");
        System.out.println(">>> ¡TEST E2E COMPLETADO CON EXITO! TODOS LOS OBJETOS PERSISTIDOS <<<");
        System.out.println("====================================================================\n");
    }

    private JsonNode post(String url, String json, int expectedStatus) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(json.isBlank() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, res.statusCode(), "POST " + url + " devolvio: " + res.body());
        return res.body().isBlank() ? null : mapper.readTree(res.body());
    }

    private void put(String url, String json, int expectedStatus) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, res.statusCode(), "PUT " + url + " devolvio: " + res.body());
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "GET " + url + " devolvio: " + res.body());
        return mapper.readTree(res.body());
    }

    private void enviarVacio(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(res.statusCode() < 300, "Request a " + url + " devolvio " + res.statusCode());
    }

    private boolean serviciosDisponibles() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(DONACIONES_URL + "/categorias")).GET().build();
            return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
