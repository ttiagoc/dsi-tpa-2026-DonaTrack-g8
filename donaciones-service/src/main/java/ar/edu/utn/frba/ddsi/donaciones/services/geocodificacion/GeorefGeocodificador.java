package ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Geocodifica con la API Georef del Estado argentino (apis.datos.gob.ar/georef), gratis y pensada para
 * direcciones argentinas. La direccion se escribe en un solo campo, "Calle y número, localidad, provincia":
 * la calle con su altura va al parametro "direccion" y el resto sirve para elegir entre las calles con el
 * mismo nombre en distintos lugares del pais (Av. Eva Peron 4200 existe en 9 lugares).
 */
@Component
public class GeorefGeocodificador implements Geocodificador {

    private final RestTemplate http;
    private final String url;

    public GeorefGeocodificador(
            @Value("${georef.url:https://apis.datos.gob.ar/georef/api}") String url) {
        SimpleClientHttpRequestFactory conexion = new SimpleClientHttpRequestFactory();
        conexion.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        conexion.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.http = new RestTemplate(conexion);
        this.url = url;
    }

    @Override
    public Coordenadas ubicar(String direccion) {
        for (Map<String, String> filtros : consultas(direccion)) {
            Optional<Coordenadas> encontrada = consultar(filtros);
            if (encontrada.isPresent()) {
                return encontrada.get();
            }
        }
        throw DireccionInvalidaException.inexistente(direccion);
    }

    /**
     * Las consultas a probar, de la mas precisa a la mas amplia. La primera que encuentre algo gana
     * (Georef ordena los resultados por parecido). La localidad puede ser un barrio de CABA, que para
     * Georef no es una localidad: por eso se prueba tambien solo con la provincia.
     */
    static List<Map<String, String>> consultas(String direccion) {
        List<String> partes = direccion == null ? List.of() : Arrays.stream(direccion.split(","))
                .map(String::trim).filter(p -> !p.isEmpty()).toList();
        if (partes.size() < 2 || !partes.get(0).matches(".*\\d.*")) {
            throw DireccionInvalidaException.incompleta();
        }
        String calle = partes.get(0).replaceFirst("(?i)^calle\\s+", "");
        List<Map<String, String>> consultas = new ArrayList<>();
        if (partes.size() == 2) {
            String lugar = partes.get(1);
            consultas.add(filtros(calle, "provincia", lugar));
            consultas.add(filtros(calle, "localidad", lugar));
            consultas.add(filtros(calle, "departamento", lugar));
        } else {
            String provincia = partes.get(partes.size() - 1);
            String lugar = partes.get(partes.size() - 2);
            consultas.add(filtros(calle, "provincia", provincia, "localidad", lugar));
            consultas.add(filtros(calle, "provincia", provincia, "departamento", lugar));
            consultas.add(filtros(calle, "provincia", provincia));
        }
        return consultas;
    }

    private static Map<String, String> filtros(String calle, String... claveValor) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("direccion", calle);
        for (int i = 0; i < claveValor.length; i += 2) {
            filtros.put(claveValor[i], claveValor[i + 1]);
        }
        return filtros;
    }

    private Optional<Coordenadas> consultar(Map<String, String> filtros) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(url).path("/direcciones").queryParam("max", 5);
        filtros.forEach(uri::queryParam);
        RespuestaGeoref respuesta;
        try {
            respuesta = http.getForObject(uri.encode().build().toUri(), RespuestaGeoref.class);
        } catch (HttpClientErrorException e) {
            return Optional.empty(); // Georef no entendio algun filtro: para esta consulta, no hay resultados
        } catch (RestClientException e) {
            throw new GeocodificadorNoDisponibleException("No se pudo consultar Georef: " + e.getMessage(), e);
        }
        if (respuesta == null || respuesta.direcciones() == null) {
            return Optional.empty();
        }
        return respuesta.direcciones().stream()
                .filter(d -> d.ubicacion() != null && d.ubicacion().lat() != null && d.ubicacion().lon() != null)
                .findFirst()
                .map(d -> new Coordenadas(d.ubicacion().lat(), d.ubicacion().lon(), d.nomenclatura()));
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaGeoref(List<DireccionGeoref> direcciones) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DireccionGeoref(String nomenclatura, Ubicacion ubicacion) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ubicacion(Double lat, Double lon) {
    }
}
