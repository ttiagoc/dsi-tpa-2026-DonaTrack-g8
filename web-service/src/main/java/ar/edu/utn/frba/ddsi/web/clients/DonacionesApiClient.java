package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.Rubro;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionAltaRequest;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.EntidadConEntregas;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteResponse;
import ar.edu.utn.frba.ddsi.web.dto.donantes.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.matchmaking.EstadoPropuestaRequest;
import ar.edu.utn.frba.ddsi.web.dto.matchmaking.PropuestaMatchmakingResponse;
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

    // ---- Mapa de entregas (publico, A, req. 2) ----

    public EntidadConEntregas[] entregasPorEntidad() {
        return getPublico("/donaciones/entregadas", EntidadConEntregas[].class);
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

    // ---- Confirmar la recepcion de una donacion (A, req. 12) ----

    /** La API devuelve 403 si la donacion no esta asignada a la entidad logueada. */
    public DonacionResponse obtenerDonacion(Context ctx, Long id) {
        return get(ctx, "/donaciones/" + id, DonacionResponse.class);
    }

    public DonacionResponse confirmarRecepcion(Context ctx, Long id, List<String> fotosUrl) {
      return post(ctx, "/donaciones/" + id + "/recepcion", Map.of("fotosUrl", fotosUrl), DonacionResponse.class);
    }

    public List<Map<String, Object>> buscarDonaciones(Context ctx, Long donanteId, String estado, Long categoriaId) {
        StringBuilder query = new StringBuilder("/donaciones?");
        if (donanteId != null) query.append("donanteId=").append(donanteId).append("&");
        if (estado != null && !estado.isBlank()) query.append("estado=").append(estado).append("&");
        if (categoriaId != null) query.append("categoriaId=").append(categoriaId).append("&");
        return get(ctx, query.toString(), new TypeReference<List<Map<String, Object>>>() {});
    }
    public List<Map<String, Object>> obtenerCategorias(Context ctx) {
        return get(ctx, "/categorias", new TypeReference<List<Map<String, Object>>>() {});
    }
    public List<Map<String, Object>> obtenerEntidades(Context ctx) {
        return get(ctx, "/entidad-beneficiaria", new TypeReference<List<Map<String, Object>>>() {});
    }
    public Map<String, Object> obtenerIncentivos(Context ctx, Long donanteId) {
        return get(ctx, "/donantes/" + donanteId + "/incentivos", new TypeReference<Map<String, Object>>() {});
    }

    // ---- Administracion: Deposito y Vencimientos (C, req. 16) ----

    public List<DonacionResponse> listarDonaciones(Context ctx, String estado) {
        String ruta = (estado != null && !estado.isBlank())
                ? "/donaciones?estado=" + estado
                : "/donaciones";
        return get(ctx, ruta, new TypeReference<List<DonacionResponse>>() {});
    }

    public DonacionResponse cambiarEstado(Context ctx, Long id, String estado, String justificacion) {
        return put(ctx, "/donaciones/" + id + "/estado", new EstadoDonacionRequest(estado, justificacion), DonacionResponse.class);
    }

    // ---- Administracion: Matchmaking y Asignaciones (C, req. 17) ----

    public List<PropuestaMatchmakingResponse> obtenerPropuestasPendientes(Context ctx) {
        return get(ctx, "/matchmaking/pendientes", new TypeReference<List<PropuestaMatchmakingResponse>>() {});
    }

    public void ejecutarMatchmaking(Context ctx) {
        post(ctx, "/matchmaking/ejecuciones", Map.of(), Void.class);
    }

    public void aceptarPropuesta(Context ctx, Long propuestaId, Long entidadId) {
        put(ctx, "/matchmaking/propuestas/" + propuestaId + "/estado", new EstadoPropuestaRequest("ACEPTADO", entidadId), Void.class);
    }

    public void rechazarPropuesta(Context ctx, Long propuestaId) {
        put(ctx, "/matchmaking/propuestas/" + propuestaId + "/estado", new EstadoPropuestaRequest("RECHAZADO", null), Void.class);
    }

    // ---- Administracion: Recepcion en Deposito (C, req. 15) ----

    public List<DonanteResponse> listarDonantes(Context ctx) {
        return get(ctx, "/donantes", new TypeReference<List<DonanteResponse>>() {});
    }

    public DonanteResponse crearDonantePersonaHumana(Context ctx, PersonaHumanaRequest request) {
        return post(ctx, "/donantes/persona-humana", request, DonanteResponse.class);
    }

    public DonanteResponse crearDonantePersonaJuridica(Context ctx, ar.edu.utn.frba.ddsi.web.dto.donantes.PersonaJuridicaAdminRequest request) {
        return post(ctx, "/donantes/persona-juridica", request, DonanteResponse.class);
    }

    public List<DonacionResponse> registrarDonacionPresencial(Context ctx, DonacionAltaRequest request) {
        return postList(ctx, "/donaciones", request, DonacionResponse.class);
    }

    // ---- Administracion: Importacion Masiva CSV (C, req. 19) ----

    public List<ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse> listarImportaciones(Context ctx) {
        return get(ctx, "/donantes/importaciones", new TypeReference<List<ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse>>() {});
    }

    public ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse obtenerImportacion(Context ctx, String id) {
        return get(ctx, "/donantes/importaciones/" + id, ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse.class);
    }

    public ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse iniciarImportacion(Context ctx, byte[] bytes, String filename) {
        return postCsv(ctx, "/donantes/importaciones", bytes, filename, ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse.class);
    }
}
