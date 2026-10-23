package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resto de la API de donaciones-service (donaciones, donantes, entidades, categorias).
 * Cada persona agrega aca los metodos que usan sus pantallas, por ejemplo:
 *
 *   public DonacionResponse obtenerDonacion(Context ctx, Long id) {
 *       return get(ctx, "/donaciones/" + id, DonacionResponse.class);
 *   }
 */
public class DonacionesApiClient extends ApiClient {

    public DonacionesApiClient(String donacionesUrl, HttpClient http, ObjectMapper mapper, AuthApiClient auth) {
        super(donacionesUrl, http, mapper, auth);
    }
}
