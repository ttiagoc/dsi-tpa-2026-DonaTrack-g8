package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient;
import ar.edu.utn.frba.ddsi.web.clients.NotificacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient.RutaSeguimientoDto;
import ar.edu.utn.frba.ddsi.web.clients.NotificacionesApiClient.NotificacionDto;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.http.Context;

@Component
public class DonanteWebController {

  private final DonacionesApiClient donacionesApi;
  private final NotificacionesApiClient notificacionesApi;
  private final LogisticaApiClient logisticaApi;

  public DonanteWebController(
      DonacionesApiClient donacionesApi,
      NotificacionesApiClient notificacionesApi,
      LogisticaApiClient logisticaApi) {
    this.donacionesApi = donacionesApi;
    this.notificacionesApi = notificacionesApi;
    this.logisticaApi = logisticaApi;
  }

  // para filtrar mis donaciones
  public void misDonaciones(Context ctx) {
    Long donanteId = obtenerPerfilId(ctx);
    String estado = ctx.queryParam("estado");
    String catStr = ctx.queryParam("categoriaId");
    Long categoriaId = (catStr != null && !catStr.isBlank()) ? Long.parseLong(catStr) : null;

    List<Map<String, Object>> donaciones = donacionesApi.buscarDonaciones(ctx, donanteId, estado, categoriaId);
    List<Map<String, Object>> categorias = donacionesApi.obtenerCategorias(ctx);

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("donaciones", donaciones);
    modelo.put("categorias", categorias);
    modelo.put("estadoSeleccionado", estado);
    modelo.put("categoriaSeleccionada", categoriaId);

    ctx.render("donante/mis-donaciones.html.hbs", modelo);
  }

  // para el catalogo de entidades
  public void entidades(Context ctx) {
    List<Map<String, Object>> entidades = donacionesApi.obtenerEntidades(ctx);

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("entidades", entidades);

    ctx.render("donante/entidades.html.hbs", modelo);
  }

  // para los incentivos
  public void incentivos(Context ctx) {
    Long donanteId = obtenerPerfilId(ctx);
    Map<String, Object> incentivos = donacionesApi.obtenerIncentivos(ctx, donanteId);

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("incentivos", incentivos);

    ctx.render("donante/incentivos.html.hbs", modelo);
  }

  // para las notificaiones del donante
  public void notificacionesDonante(Context ctx) {
    Long donanteId = obtenerPerfilId(ctx);
    List<NotificacionDto> notificaciones = notificacionesApi.obtenerNotificaciones(ctx, donanteId, "DONANTE");

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("notificaciones", notificaciones);
    modelo.put("esDonante", true);

    ctx.render("donante/notificaciones.html.hbs", modelo);
  }

  // para las notificaciones de la entidad
  public void notificacionesEntidad(Context ctx) {
    Long entidadId = obtenerPerfilId(ctx);
    List<NotificacionDto> notificaciones = notificacionesApi.obtenerNotificaciones(ctx, entidadId, "ENTIDAD");

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("notificaciones", notificaciones);
    modelo.put("esEntidad", true);

    ctx.render("entidad/notificaciones.html.hbs", modelo);
  }

  // para el seguimiento en mapa del donante
  public void seguimientoDonante(Context ctx) {
    Long donanteId = obtenerPerfilId(ctx);
    List<Map<String, Object>> donaciones = donacionesApi.buscarDonaciones(ctx, donanteId, null, null);

    List<Long> donacionIds = new ArrayList<>();
    if (donaciones != null) {
      for (Map<String, Object> d : donaciones) {
        Object idObj = d.get("id");
        if (idObj instanceof Number n) {
          donacionIds.add(n.longValue());
        }
      }
    }

    List<RutaSeguimientoDto> rutas = logisticaApi.obtenerRutasActivasDonante(ctx, donacionIds);

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("rutas", rutas);
    modelo.put("tipoRol", "Donante");

    ctx.render("donante/seguimiento.html.hbs", modelo);
  }

  // para el seguimiento en mapa de la entidad
  public void seguimientoEntidad(Context ctx) {
    Long entidadId = obtenerPerfilId(ctx);
    List<RutaSeguimientoDto> rutas = logisticaApi.obtenerRutasActivasEntidad(ctx, entidadId);

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("rutas", rutas);
    modelo.put("tipoRol", "Entidad");

    ctx.render("entidad/seguimiento.html.hbs", modelo);
  }

  private Long obtenerPerfilId(Context ctx) {
    return SesionUsuario.actual(ctx)
        .map(s -> s.getUsuario().perfilId())
        .orElse(null);
  }
}