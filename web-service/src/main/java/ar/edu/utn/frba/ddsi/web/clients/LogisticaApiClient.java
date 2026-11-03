package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.Context;

public class LogisticaApiClient extends ApiClient {

  public LogisticaApiClient(String logisticaUrl, HttpClient http, ObjectMapper mapper, AuthApiClient auth) {
    super(logisticaUrl, http, mapper, auth);
  }

  public List<RutaSeguimientoDto> obtenerRutasActivasEntidad(Context ctx, Long entidadId) {
    return get(ctx, "/rutas/activas?entidadId=" + entidadId, new TypeReference<List<RutaSeguimientoDto>>() {});
  }

  public List<RutaSeguimientoDto> obtenerRutasActivasDonante(Context ctx, List<Long> donacionIds) {
    if (donacionIds == null || donacionIds.isEmpty()) {
      return List.of();
    }
    String idsStr = String.join(",", donacionIds.stream().map(String::valueOf).toList());
    return get(ctx, "/rutas/activas?donacionIds=" + idsStr, new TypeReference<List<RutaSeguimientoDto>>() {});
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
      String estado
  ) {}
}