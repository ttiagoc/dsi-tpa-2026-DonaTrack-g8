package ar.edu.utn.frba.ddsi.web.clients;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

import ar.edu.utn.frba.ddsi.web.exceptions.AccesoDenegadoException;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiNoDisponibleException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionRequeridaException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionVencidaException;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.http.Context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Base de todos los clientes de las APIs. Un XApiClient nuevo extiende esta clase y usa get/post/put/delete.
 * - Las llamadas con Context van autenticadas: mandan "Authorization: Bearer" con el token de la sesion.
 * - Ante un 401 pide tokens nuevos (/api/auth/refresh) y reintenta una sola vez; si no puede, la sesion vencio.
 * - Los errores del backend ({error, message}) se convierten en excepciones con el mensaje para mostrar.
 */
public abstract class ApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final String baseUrl;
    private final HttpClient http;
    protected final ObjectMapper mapper;
    private final AuthApiClient auth;

    protected ApiClient(String baseUrl, HttpClient http, ObjectMapper mapper, AuthApiClient auth) {
        this.baseUrl = baseUrl;
        this.http = http;
        this.mapper = mapper;
        this.auth = auth;
    }

    public static ObjectMapper crearObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** Quien renueva los tokens; AuthApiClient se devuelve a si mismo. */
    protected AuthApiClient auth() {
        return auth;
    }

    /** Id del donante o de la entidad logueada: el que va en las rutas de "lo mio". */
    protected Long perfilId(Context ctx) {
        return SesionUsuario.actual(ctx).orElseThrow(SesionRequeridaException::new).getUsuario().perfilId();
    }

    // ---- Llamadas autenticadas (usan la sesion del usuario) ----

    protected <T> T get(Context ctx, String ruta, Class<T> tipo) {
        return leer(enviarAutenticado(ctx, () -> request(ruta).GET()), tipo);
    }

    protected <T> T get(Context ctx, String ruta, TypeReference<T> tipo) {
        HttpResponse<String> respuesta = enviarAutenticado(ctx, () -> request(ruta).GET());
        verificar(respuesta);
        return convertir(respuesta.body(), tipo);
    }

    protected <T> T post(Context ctx, String ruta, Object body, Class<T> tipo) {
        String json = aJson(body);
        return leer(enviarAutenticado(ctx, () -> conJson(request(ruta), "POST", json)), tipo);
    }

    protected <T> T put(Context ctx, String ruta, Object body, Class<T> tipo) {
        String json = aJson(body);
        return leer(enviarAutenticado(ctx, () -> conJson(request(ruta), "PUT", json)), tipo);
    }

    protected void delete(Context ctx, String ruta) {
        leer(enviarAutenticado(ctx, () -> request(ruta).DELETE()), Void.class);
    }

    // ---- Llamadas publicas (login, registro, categorias) ----

    protected <T> T getPublico(String ruta, Class<T> tipo) {
        return leer(enviar(request(ruta).GET()), tipo);
    }

    protected <T> T postPublico(String ruta, Object body, Class<T> tipo) {
        return leer(enviar(conJson(request(ruta), "POST", aJson(body))), tipo);
    }

    // ---- Internos ----

    private HttpResponse<String> enviarAutenticado(Context ctx, Supplier<HttpRequest.Builder> pedido) {
        SesionUsuario sesion = SesionUsuario.actual(ctx).orElseThrow(SesionRequeridaException::new);

        HttpResponse<String> respuesta = enviar(conToken(pedido.get(), sesion));
        if (respuesta.statusCode() != 401) {
            return respuesta;
        }

        auth().renovarTokens(ctx, sesion);
        respuesta = enviar(conToken(pedido.get(), sesion));
        if (respuesta.statusCode() == 401) {
            SesionUsuario.cerrar(ctx);
            throw new SesionVencidaException();
        }
        return respuesta;
    }

    private HttpRequest.Builder conToken(HttpRequest.Builder builder, SesionUsuario sesion) {
        return builder.header("Authorization", "Bearer " + sesion.getAccessToken());
    }

    private HttpRequest.Builder request(String ruta) {
        return HttpRequest.newBuilder(URI.create(baseUrl + ruta))
                .timeout(TIMEOUT)
                .header("Accept", "application/json");
    }

    private HttpRequest.Builder conJson(HttpRequest.Builder builder, String metodo, String json) {
        return builder.header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpResponse<String> enviar(HttpRequest.Builder builder) {
        HttpRequest pedido = builder.build();
        try {
            return http.send(pedido, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiNoDisponibleException("No se pudo conectar con " + pedido.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiNoDisponibleException("Se interrumpio la llamada a " + pedido.uri(), e);
        }
    }

    private <T> T leer(HttpResponse<String> respuesta, Class<T> tipo) {
        verificar(respuesta);
        if (tipo == Void.class || respuesta.body() == null || respuesta.body().isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(respuesta.body(), tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta inesperada de la API: " + respuesta.body(), e);
        }
    }

    private <T> T convertir(String body, TypeReference<T> tipo) {
        try {
            return mapper.readValue(body, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta inesperada de la API: " + body, e);
        }
    }

    private void verificar(HttpResponse<String> respuesta) {
        int status = respuesta.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String mensaje = mensajeDeError(respuesta.body(), status);
        if (status == 403) {
            throw new AccesoDenegadoException(mensaje);
        }
        throw new ApiException(status, mensaje);
    }

    private String mensajeDeError(String body, int status) {
        try {
            JsonNode json = mapper.readTree(body);
            if (json != null && json.hasNonNull("message")) {
                return json.get("message").asText();
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // el cuerpo no es JSON: se usa el mensaje generico
        }
        return "La API respondio con error " + status;
    }

    private String aJson(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo convertir a JSON: " + body, e);
        }
    }
}
