package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;
import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.Context;

public class NotificacionesApiClient extends ApiClient {

  private static final ZoneId ZONA_UTC = ZoneId.of("UTC");
  private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public NotificacionesApiClient(String notificacionesUrl, HttpClient http, ObjectMapper mapper, AuthApiClient auth) {
      super(notificacionesUrl, http, mapper, auth);
    }

    public List<NotificacionDto> obtenerNotificaciones(Context ctx, Long destinatarioId, String tipoDestinatario) {
      String path = "/notificaciones?destinatarioId=" + destinatarioId + "&tipoDestinatario=" + tipoDestinatario;
      return get(ctx, path, new TypeReference<List<NotificacionDto>>() {});
    }

  public record NotificacionDto(
      Long id,
      String fechaDeEnvio,
      String mensaje,
      String tipoEvento,
      String tipoDestinatario,
      Long destinatarioId,
      Long donacionId,
      Boolean completada
  ) {
    public String mensajeLimpio() {
      if (mensaje == null || mensaje.isBlank()) return "-";
      String limpio = mensaje;
      String urlWeb = "DONANTE".equalsIgnoreCase(tipoDestinatario) ? "/donante/seguimiento" : "/entidad/seguimiento";
      limpio = limpio.replaceAll(":\\s*https?://[^\\s]+/api/monitoreo/ubicacion/\\d+", " en tiempo real desde la sección Seguimiento.");
      limpio = limpio.replaceAll("https?://[^\\s]+/api/monitoreo/ubicacion/\\d+", urlWeb);
      limpio = limpio.replaceAll("https?://logistica-service:8081[^\\s]*", urlWeb);
      return limpio;
    }

    public String accionUrl() {
      if ("EN_CAMINO".equalsIgnoreCase(tipoEvento) || "INICIO_RUTA".equalsIgnoreCase(tipoEvento)) {
        return "DONANTE".equalsIgnoreCase(tipoDestinatario) ? "/donante/seguimiento" : "/entidad/seguimiento";
      }
      return null;
    }

    public String accionTexto() {
      if ("EN_CAMINO".equalsIgnoreCase(tipoEvento) || "INICIO_RUTA".equalsIgnoreCase(tipoEvento)) {
        return "Ver en el mapa";
      }
      return null;
    }

    public String fechaFormateada() {
      if (fechaDeEnvio == null || fechaDeEnvio.isBlank()) return "-";
      try {
        String limpia = fechaDeEnvio.trim().replace(" ", "T");
        if (limpia.endsWith("Z")) {
          return Instant.parse(limpia).atZone(ZONA_ARGENTINA).format(FORMATO_FECHA_HORA);
        }
        if (limpia.contains("+") || (limpia.lastIndexOf('-') > limpia.indexOf('T'))) {
          return OffsetDateTime.parse(limpia).atZoneSameInstant(ZONA_ARGENTINA).format(FORMATO_FECHA_HORA);
        }
        LocalDateTime utc = LocalDateTime.parse(limpia);
        return utc.atZone(ZONA_UTC).withZoneSameInstant(ZONA_ARGENTINA).format(FORMATO_FECHA_HORA);
      } catch (Exception e) {
        return fechaDeEnvio;
      }
    }

    public String eventoLegible() {
      if (tipoEvento == null || tipoEvento.isBlank()) return null;
      return switch (tipoEvento) {
        case "DONACION_ASIGNADA" -> "Donación asignada";
        case "ALTA_USUARIO" -> "Cuenta creada";
        case "ALTA_IMPORTACION" -> "Importación";
        case "EN_CAMINO", "INICIO_RUTA" -> "En camino";
        case "ENTREGA_EXITOSA" -> "Entrega exitosa";
        case "ENTREGA_FALLIDA" -> "Entrega fallida";
        case "RECEPCION_CONFIRMADA" -> "Recepción confirmada";
        case "AUSENCIA_PLATAFORMA" -> "Recordatorio";
        default -> tipoEvento;
      };
    }

    public String claseEtiqueta() {
      if (tipoEvento == null) return "etiqueta--neutra";
      return switch (tipoEvento) {
        case "DONACION_ASIGNADA", "ALTA_USUARIO", "RECEPCION_CONFIRMADA", "ENTREGA_EXITOSA" -> "etiqueta--ok";
        case "EN_CAMINO", "INICIO_RUTA" -> "etiqueta--info";
        case "ENTREGA_FALLIDA" -> "etiqueta--error";
        case "AUSENCIA_PLATAFORMA" -> "etiqueta--pendiente";
        default -> "etiqueta--neutra";
      };
    }
  }
}