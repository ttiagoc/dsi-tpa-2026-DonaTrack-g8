package ar.edu.utn.frba.ddsi.web.clients;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaConSubcategorias;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaRequest;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.UsosCatalogoResponse;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.Rubro;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionAltaRequest;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DireccionUbicada;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.EntidadConEntregas;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteDetalle;
import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteResponse;
import ar.edu.utn.frba.ddsi.web.dto.donantes.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.PersonaJuridicaRequest;
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
                .map(categoria -> new Rubro(categoria.nombre(), categoria.pideEstado(), categoria.esPerecedero(),
                        Arrays.stream(getPublico("/categorias/" + categoria.id() + "/subcategorias",
                                SubcategoriaResponse[].class)).map(SubcategoriaResponse::nombre).toList()))
                .filter(rubro -> !rubro.subcategorias().isEmpty())
                .toList();
    }

    // ---- ABM de categorias y subcategorias (admin) ----

    /** Todas las categorias, cada una con sus subcategorias (tambien las que todavia no tienen ninguna). */
    public List<CategoriaConSubcategorias> catalogoCompleto(Context ctx) {
        List<SubcategoriaResponse> subcategorias = obtenerSubcategorias(ctx, null);
        return get(ctx, "/categorias", new TypeReference<List<CategoriaResponse>>() {}).stream()
                .map(categoria -> new CategoriaConSubcategorias(categoria, subcategorias.stream()
                        .filter(sub -> categoria.id().equals(sub.categoriaId()))
                        .toList()))
                .toList();
    }

    public CategoriaResponse crearCategoria(Context ctx, CategoriaRequest request) {
        return post(ctx, "/categorias", request, CategoriaResponse.class);
    }

    public CategoriaResponse actualizarCategoria(Context ctx, Long id, CategoriaRequest request) {
        return put(ctx, "/categorias/" + id, request, CategoriaResponse.class);
    }

    public SubcategoriaResponse crearSubcategoria(Context ctx, Long categoriaId, SubcategoriaRequest request) {
        return post(ctx, "/categorias/" + categoriaId + "/subcategorias", request, SubcategoriaResponse.class);
    }

    public SubcategoriaResponse actualizarSubcategoria(Context ctx, Long id, SubcategoriaRequest request) {
        return put(ctx, "/subcategorias/" + id, request, SubcategoriaResponse.class);
    }

    public UsosCatalogoResponse usosCategoria(Context ctx, Long id) {
        return get(ctx, "/categorias/" + id + "/usos", UsosCatalogoResponse.class);
    }

    public UsosCatalogoResponse usosSubcategoria(Context ctx, Long id) {
        return get(ctx, "/subcategorias/" + id + "/usos", UsosCatalogoResponse.class);
    }

    /** Borra la categoria con todas sus subcategorias; la API lo rechaza si alguna esta en uso. */
    public void eliminarCategoria(Context ctx, Long id) {
        delete(ctx, "/categorias/" + id);
    }

    public void eliminarSubcategoria(Context ctx, Long id) {
        delete(ctx, "/subcategorias/" + id);
    }

    // ---- Mapa de entregas (publico, A, req. 2) ----

    public EntidadConEntregas[] entregasPorEntidad() {
        return getPublico("/donaciones/entregadas", EntidadConEntregas[].class);
    }

    /** Busca la direccion en Georef (a traves de la API). ApiException 400 si no existe, 503 si Georef no responde. */
    public DireccionUbicada ubicarDireccion(String direccion) {
        return getPublico("/direcciones/ubicar?direccion=" + URLEncoder.encode(direccion, StandardCharsets.UTF_8),
                DireccionUbicada.class);
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
        return buscarDonaciones(ctx, donanteId, estado, categoriaId, null);
    }

    public List<Map<String, Object>> buscarDonaciones(Context ctx, Long donanteId, String estado, Long categoriaId, List<Long> subcategoriaIds) {
        StringBuilder query = new StringBuilder("/donaciones?");
        if (donanteId != null) query.append("donanteId=").append(donanteId).append("&");
        if (estado != null && !estado.isBlank()) query.append("estado=").append(estado).append("&");
        if (categoriaId != null) query.append("categoriaId=").append(categoriaId).append("&");
        if (subcategoriaIds != null && !subcategoriaIds.isEmpty()) {
            query.append("subcategoriaIds=")
                    .append(subcategoriaIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .append("&");
        }
        return get(ctx, query.toString(), new TypeReference<List<Map<String, Object>>>() {});
    }

    public List<SubcategoriaResponse> obtenerSubcategorias(Context ctx, Long categoriaId) {
        if (categoriaId != null) {
            return get(ctx, "/categorias/" + categoriaId + "/subcategorias", new TypeReference<List<SubcategoriaResponse>>() {});
        }
        return get(ctx, "/subcategorias", new TypeReference<List<SubcategoriaResponse>>() {});
    }
    public List<Map<String, Object>> obtenerCategorias(Context ctx) {
        return get(ctx, "/categorias", new TypeReference<List<Map<String, Object>>>() {});
    }
    public List<Map<String, Object>> obtenerEntidades(Context ctx) {
        return get(ctx, "/entidad-beneficiaria", new TypeReference<List<Map<String, Object>>>() {});
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

    public DonanteResponse crearDonantePersonaJuridica(Context ctx, PersonaJuridicaRequest request) {
        return post(ctx, "/donantes/persona-juridica", request, DonanteResponse.class);
    }

    // ---- Administracion: Donantes (C, req. 15) ----

    public DonanteDetalle obtenerDetalleDonante(Context ctx, Long id) {
        return get(ctx, "/donantes/" + id + "/detalle", DonanteDetalle.class);
    }

    public DonanteResponse actualizarDonantePersonaHumana(Context ctx, Long id, PersonaHumanaRequest request) {
        return put(ctx, "/donantes/persona-humana/" + id, request, DonanteResponse.class);
    }

    public DonanteResponse actualizarDonantePersonaJuridica(Context ctx, Long id, PersonaJuridicaRequest request) {
        return put(ctx, "/donantes/persona-juridica/" + id, request, DonanteResponse.class);
    }

    /** La API responde 400 si el donante tiene donaciones registradas (se conservan por trazabilidad). */
    public void eliminarDonante(Context ctx, Long id) {
        delete(ctx, "/donantes/" + id);
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
