package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frba.ddsi.web.dto.camiones.CamionRequest;
import ar.edu.utn.frba.ddsi.web.dto.camiones.CamionResponse;
import ar.edu.utn.frba.ddsi.web.dto.comun.PaginaResponse;
import io.javalin.http.Context;

public class LogisticaApiClient extends ApiClient {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public LogisticaApiClient(String logisticaUrl, HttpClient http, ObjectMapper mapper, AuthApiClient auth) {
    super(logisticaUrl, http, mapper, auth);
  }

  public List<CamionResponse> listarCamiones(Context ctx) {
    List<CamionResponse> lista = get(ctx, "/camiones", new TypeReference<List<CamionResponse>>() {});
    return lista != null ? lista : List.of();
  }

  public PaginaResponse<CamionResponse> listarCamionesPaginado(Context ctx, int page, int size) {
    return getPaginado(ctx, "/camiones?page=" + page + "&size=" + size, CamionResponse.class, page, size);
  }

  public CamionResponse obtenerCamion(Context ctx, Long id) {
    return get(ctx, "/camiones/" + id, new TypeReference<CamionResponse>() {});
  }

  public CamionResponse crearCamion(Context ctx, CamionRequest request) {
    return post(ctx, "/camiones", request, CamionResponse.class);
  }

  public CamionResponse actualizarCamion(Context ctx, Long id, CamionRequest request) {
    return put(ctx, "/camiones/" + id, request, CamionResponse.class);
  }

  public void eliminarCamion(Context ctx, Long id) {
    delete(ctx, "/camiones/" + id);
  }

  public List<RutaDto> listarRutas(Context ctx) {
    List<RutaDto> lista = get(ctx, "/rutas", new TypeReference<List<RutaDto>>() {});
    return lista != null ? lista : List.of();
  }

  public PaginaResponse<RutaDto> listarRutasPaginado(Context ctx, int page, int size) {
    return getPaginado(ctx, "/rutas?page=" + page + "&size=" + size, RutaDto.class, page, size);
  }

  public RutaDto obtenerRuta(Context ctx, Long id) {
    return get(ctx, "/rutas/" + id, RutaDto.class);
  }

  public RutaDto obtenerRutaActivaCamion(Context ctx, Long camionId) {
    return get(ctx, "/rutas/camion/" + camionId + "/activa", RutaDto.class);
  }

  public List<RutaDto> listarRutasCamion(Context ctx, Long camionId) {
    List<RutaDto> lista = get(ctx, "/rutas/camion/" + camionId, new TypeReference<List<RutaDto>>() {});
    return lista != null ? lista : List.of();
  }

  public void planificarRutas(Context ctx) {
    post(ctx, "/rutas/planificaciones", Map.of(), Void.class);
  }

  public void cambiarEstadoRuta(Context ctx, Long rutaId, String estado) {
    put(ctx, "/rutas/" + rutaId + "/estado", estado, Void.class);
  }

  public void confirmarParada(Context ctx, Long rutaId, Long paradaId) {
    post(ctx, "/rutas/" + rutaId + "/paradas/" + paradaId + "/confirmaciones", Map.of(), Void.class);
  }

  public void reportarFalloParada(Context ctx, Long rutaId, Long paradaId, String motivo) {
    post(ctx, "/rutas/" + rutaId + "/paradas/" + paradaId + "/fallos",
        Map.of("motivo", motivo != null ? motivo : ""), Void.class);
  }

  public void actualizarUbicacionCamion(Context ctx, String patente, double latitud, double longitud, double velocidad) {
    post(ctx, "/camiones/ubicacion/" + patente,
        Map.of("latitud", latitud, "longitud", longitud, "velocidad", velocidad), Void.class);
  }

  public List<RutaSeguimientoDto> obtenerRutasActivasEntidad(Context ctx, Long entidadId) {
    List<RutaSeguimientoDto> lista = get(ctx, "/rutas/activas?entidadId=" + entidadId, new TypeReference<List<RutaSeguimientoDto>>() {});
    return lista != null ? lista : List.of();
  }

  public List<RutaSeguimientoDto> obtenerRutasActivasDonante(Context ctx, List<Long> donacionIds) {
    if (donacionIds == null || donacionIds.isEmpty()) {
      return List.of();
    }
    String idsStr = String.join(",", donacionIds.stream().map(String::valueOf).toList());
    List<RutaSeguimientoDto> lista = get(ctx, "/rutas/activas?donacionIds=" + idsStr, new TypeReference<List<RutaSeguimientoDto>>() {});
    return lista != null ? lista : List.of();
  }

  public record RutaDto(
      Long id,
      LocalDate fecha,
      String estado,
      String patenteCamion,
      List<ParadaDto> paradas,
      String choferNombre,
      String choferApellido,
      Long camionId,
      Double camionLatitud,
      Double camionLongitud
  ) {
    public String choferNombreCompleto() {
      String nombre = choferNombre != null ? choferNombre : "";
      String apellido = choferApellido != null ? choferApellido : "";
      String completo = (nombre + " " + apellido).trim();
      return completo.isEmpty() ? "-" : completo;
    }

    public String fechaFormateada() {
      return fecha != null ? fecha.format(FORMATO_FECHA) : "-";
    }

    public int cantidadParadas() {
      return paradas != null ? paradas.size() : 0;
    }

    public String paradasTexto() {
      int n = cantidadParadas();
      return n == 1 ? "1 parada" : n + " paradas";
    }

    public List<Long> donacionIds() {
      if (paradas == null) {
        return List.of();
      }
      return paradas.stream()
          .map(ParadaDto::entregas)
          .filter(Objects::nonNull)
          .flatMap(List::stream)
          .filter(Objects::nonNull)
          .distinct()
          .toList();
    }

    public int cantidadEntregas() {
      return donacionIds().size();
    }

    public String resumenEntregas() {
      List<Long> ids = donacionIds();
      if (ids.isEmpty()) {
        return "Sin entregas";
      }
      String detalle = ids.stream().map(id -> "#" + id).collect(Collectors.joining(", "));
      return (ids.size() == 1 ? "1 entrega (" : ids.size() + " entregas (") + detalle + ")";
    }

    public boolean esPlanificada() {
      return "PLANIFICADA".equalsIgnoreCase(estado);
    }

    public boolean esEnTraslado() {
      return "EN_TRASLADO".equalsIgnoreCase(estado);
    }

    public boolean esFinalizada() {
      return "FINALIZADA".equalsIgnoreCase(estado);
    }
  }

  public record RutaSeguimientoDto(
      Long rutaId,
      String patenteCamion,
      String chofer,
      Double latitud,
      Double longitud,
      Double velocidad,
      String ultimaActualizacion,
      List<ParadaDto> paradas
  ) {}

  public record ParadaDto(
      Long id,
      Integer orden,
      String destino,
      Long entidad,
      List<Long> entregas,
      String estado,
      Double latitud,
      Double longitud
  ) {
    public ParadaDto(Long id, Integer orden, String destino, Long entidad, List<Long> entregas, String estado) {
      this(id, orden, destino, entidad, entregas, estado, null, null);
    }
  }
}