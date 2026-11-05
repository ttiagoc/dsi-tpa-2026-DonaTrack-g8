package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient;
import ar.edu.utn.frba.ddsi.web.clients.NotificacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient.RutaSeguimientoDto;
import ar.edu.utn.frba.ddsi.web.clients.NotificacionesApiClient.NotificacionDto;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaResponse;
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

    List<String> rawSubcatParams = ctx.queryParams("subcategoriaIds");
    List<Long> subcategoriaIds = new ArrayList<>();
    for (String param : rawSubcatParams) {
      if (param != null && !param.isBlank()) {
        for (String part : param.split(",")) {
          part = part.trim();
          if (!part.isEmpty()) {
            try {
              subcategoriaIds.add(Long.parseLong(part));
            } catch (NumberFormatException ignored) {}
          }
        }
      }
    }
    if (subcategoriaIds.isEmpty()) {
      String singleSubcat = ctx.queryParam("subcategoriaId");
      if (singleSubcat != null && !singleSubcat.isBlank()) {
        try {
          subcategoriaIds.add(Long.parseLong(singleSubcat.trim()));
        } catch (NumberFormatException ignored) {}
      }
    }

    List<Map<String, Object>> donaciones = donacionesApi.buscarDonaciones(ctx, donanteId, estado, categoriaId,
        subcategoriaIds.isEmpty() ? null : subcategoriaIds);
    List<Map<String, Object>> categorias = donacionesApi.obtenerCategorias(ctx);
    List<SubcategoriaResponse> subcategorias = donacionesApi.obtenerSubcategorias(ctx, categoriaId);

    List<Map<String, Object>> subcategoriasView = new ArrayList<>();
    for (SubcategoriaResponse sub : subcategorias) {
      Map<String, Object> sMap = new HashMap<>();
      sMap.put("id", sub.id());
      sMap.put("nombre", sub.nombre());
      sMap.put("categoria", sub.categoria());
      sMap.put("categoriaId", sub.categoriaId());
      sMap.put("seleccionada", subcategoriaIds.contains(sub.id()));
      subcategoriasView.add(sMap);
    }

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("donaciones", donaciones);
    modelo.put("categorias", categorias);
    modelo.put("subcategorias", subcategoriasView);
    modelo.put("estadoSeleccionado", estado);
    modelo.put("categoriaSeleccionada", categoriaId);
    modelo.put("subcategoriaSeleccionada", subcategoriaIds.isEmpty() ? null : subcategoriaIds.get(0));
    modelo.put("subcategoriasSeleccionadas", subcategoriaIds);

    boolean hayFiltros = (estado != null && !estado.isBlank())
        || categoriaId != null
        || !subcategoriaIds.isEmpty();
    modelo.put("hayFiltros", hayFiltros);
    if (hayFiltros) {
      modelo.put("vacioTitulo", "Ninguna donación coincide con los filtros aplicados.");
      modelo.put("vacioBajada", "Probá cambiando o limpiando los filtros para ver otras donaciones.");
    } else {
      modelo.put("vacioTitulo", "Todavía no registraste ninguna donación.");
      modelo.put("vacioBajada", "Cuando entregues donaciones en el depósito o se registren a tu nombre, van a figurar acá.");
    }

    ctx.render("donante/mis-donaciones.html.hbs", modelo);
  }

  // para el catalogo de entidades
  public void entidades(Context ctx) {
    List<Map<String, Object>> todas = donacionesApi.obtenerEntidades(ctx);
    List<Map<String, Object>> categorias = donacionesApi.obtenerCategorias(ctx);

    String busqueda = ctx.queryParam("busqueda");
    String categoria = ctx.queryParam("categoria");

    List<Map<String, Object>> entidadesFiltradas = new ArrayList<>();
    for (Map<String, Object> ent : todas) {
      String razonSocial = (String) ent.get("razonSocial");
      String direccion = (String) ent.get("direccion");
      String telefono = (String) ent.get("telefono");
      @SuppressWarnings("unchecked")
      List<String> correos = (List<String>) ent.get("correos");
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> necesidades = (List<Map<String, Object>>) ent.get("necesidades");
      if (necesidades == null) {
        necesidades = List.of();
      }

      // Filtro busqueda (nombre o direccion)
      if (busqueda != null && !busqueda.isBlank()) {
        String bLower = busqueda.trim().toLowerCase();
        boolean coincideNombre = razonSocial != null && razonSocial.toLowerCase().contains(bLower);
        boolean coincideDir = direccion != null && direccion.toLowerCase().contains(bLower);
        if (!coincideNombre && !coincideDir) {
          continue;
        }
      }

      // Filtro categoria de necesidad
      if (categoria != null && !categoria.isBlank()) {
        boolean tieneCategoria = necesidades.stream().anyMatch(n -> {
          String cat = (String) n.get("categoria");
          return cat != null && cat.equalsIgnoreCase(categoria.trim());
        });
        if (!tieneCategoria) {
          continue;
        }
      }

      Map<String, Object> entView = new HashMap<>(ent);
      entView.put("razonSocial", razonSocial);
      entView.put("direccion", direccion);
      entView.put("telefono", telefono);
      entView.put("correos", correos);
      entView.put("necesidades", necesidades);
      entView.put("totalNecesidades", necesidades.size());

      List<Map<String, Object>> primeras = necesidades.stream().limit(2).toList();
      entView.put("primerasNecesidades", primeras);
      int restantes = Math.max(0, necesidades.size() - 2);
      entView.put("masNecesidades", restantes);
      entView.put("tieneMasNecesidades", restantes > 0);

      entidadesFiltradas.add(entView);
    }

    Map<String, Object> modelo = Modelo.base(ctx);
    modelo.put("entidades", entidadesFiltradas);
    modelo.put("categorias", categorias);
    modelo.put("busqueda", busqueda != null ? busqueda : "");
    modelo.put("categoriaSeleccionada", categoria != null ? categoria : "");

    boolean hayFiltros = (busqueda != null && !busqueda.isBlank()) || (categoria != null && !categoria.isBlank());
    modelo.put("hayFiltros", hayFiltros);
    if (hayFiltros) {
      modelo.put("vacioTitulo", "No se encontraron entidades coincidentes.");
      modelo.put("vacioBajada", "Probá cambiando el texto de búsqueda o la categoría seleccionada.");
    } else {
      modelo.put("vacioTitulo", "Todavía no hay entidades registradas.");
      modelo.put("vacioBajada", "Cuando las entidades se sumen a DonaTrack y registren necesidades, van a figurar acá.");
    }

    ctx.render("donante/entidades.html.hbs", modelo);
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