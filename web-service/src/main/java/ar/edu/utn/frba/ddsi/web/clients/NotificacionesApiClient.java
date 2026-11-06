package ar.edu.utn.frba.ddsi.web.clients;

import java.net.http.HttpClient;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.Context;

public class NotificacionesApiClient extends ApiClient {

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
        if (fechaDeEnvio.contains("T")) {
          String[] partes = fechaDeEnvio.split("T");
          String fecha = partes[0];
          String hora = partes[1].substring(0, Math.min(5, partes[1].length()));
          return fecha + " " + hora;
        }
        return fechaDeEnvio;
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