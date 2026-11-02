package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.Rubro;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.dto.necesidades.NecesidadRequest;
import ar.edu.utn.frba.ddsi.web.dto.necesidades.NecesidadResponse;
import io.javalin.http.Context;

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

    // ---- Catalogo (publico) ----

    /** Categorias con sus subcategorias, para los selects agrupados. */
    public List<Rubro> catalogo() {
        return Arrays.stream(getPublico("/categorias", CategoriaResponse[].class))
                .map(categoria -> new Rubro(categoria.nombre(),
                        Arrays.stream(getPublico("/categorias/" + categoria.id() + "/subcategorias",
                                SubcategoriaResponse[].class)).map(SubcategoriaResponse::nombre).toList()))
                .filter(rubro -> !rubro.subcategorias().isEmpty())
                .toList();
    }

    // ---- Necesidades de la entidad logueada (A, req. 10) ----

    public List<NecesidadResponse> listarNecesidades(Context ctx) {
        return get(ctx, rutaNecesidades(ctx), new TypeReference<List<NecesidadResponse>>() {
        });
    }

    public NecesidadResponse registrarNecesidad(Context ctx, NecesidadRequest necesidad) {
        return post(ctx, rutaNecesidades(ctx), necesidad, NecesidadResponse.class);
    }

    public void eliminarNecesidad(Context ctx, Long necesidadId) {
        delete(ctx, rutaNecesidades(ctx) + "/" + necesidadId);
    }

    private String rutaNecesidades(Context ctx) {
        return "/entidad-beneficiaria/" + perfilId(ctx) + "/necesidades";
    }

    // ---- Donaciones asignadas a la entidad logueada (A, req. 11) ----

    public List<DonacionResponse> listarDonacionesAsignadas(Context ctx) {
        return get(ctx, "/donaciones?entidadId=" + perfilId(ctx), new TypeReference<List<DonacionResponse>>() {
        });
    }
}
