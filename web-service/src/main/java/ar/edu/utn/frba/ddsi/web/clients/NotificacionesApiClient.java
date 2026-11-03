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
  }
}