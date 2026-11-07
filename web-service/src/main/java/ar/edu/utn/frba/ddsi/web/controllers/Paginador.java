package ar.edu.utn.frba.ddsi.web.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ar.edu.utn.frba.ddsi.web.dto.comun.PaginaResponse;
import io.javalin.http.Context;

/**
 * Utilidad de paginacion para los controllers del Cliente Liviano (web-service).
 * Construye el mapa para la vista Handlebars y arma las URLs limpias
 * preservando filtros de busqueda y estado, pero descartando parametros efimeros de modales.
 */
public final class Paginador {

    private static final Set<String> PARAMETROS_EFIMEROS = Set.of(
            "editar", "eliminar", "nuevo", "nueva", "editarSub", "nuevaSub",
            "vencer", "confirmarParada", "fallarParada", "logout", "vencida", "ver", "propuesta", "activa"
    );

    private Paginador() {}

    /**
     * Construye el mapa de paginacion para el modelo a partir de un PaginaResponse del backend.
     */
    public static Map<String, Object> armarModelo(Context ctx, PaginaResponse<?> pagina, String rutaBase) {
        Map<String, Object> map = new HashMap<>();
        map.put("pagina", pagina.pagina());
        map.put("totalPaginas", pagina.totalPaginas());
        map.put("desde", pagina.desde());
        map.put("hasta", pagina.hasta());
        map.put("total", pagina.totalElementos());
        map.put("hayVarias", pagina.hayVarias());
        if (pagina.tieneAnterior()) {
            map.put("anterior", urlConPagina(rutaBase, ctx.queryParamMap(), pagina.pagina() - 1));
        }
        if (pagina.tieneSiguiente()) {
            map.put("siguiente", urlConPagina(rutaBase, ctx.queryParamMap(), pagina.pagina() + 1));
        }
        return map;
    }

    /**
     * Pagina una lista en memoria (para colecciones que se calculan o agregan en el web-service).
     */
    public static <T> PaginaResponse<T> paginarLista(Context ctx, List<T> todos, int porPagina) {
        int paginaParam = entero(ctx.queryParam("pagina"), 1);
        int total = todos != null ? todos.size() : 0;
        int tamanoSeguro = Math.max(1, porPagina);
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamanoSeguro));
        int paginaSegura = Math.min(Math.max(1, paginaParam), totalPaginas);

        int desde = (paginaSegura - 1) * tamanoSeguro;
        List<T> elementos = (todos == null || todos.isEmpty())
                ? List.of()
                : todos.subList(Math.min(desde, total), Math.min(desde + tamanoSeguro, total));

        return new PaginaResponse<>(elementos, paginaSegura, totalPaginas, total, tamanoSeguro);
    }

    /**
     * Genera la URL para una pagina determinada preservando los query params existentes.
     */
    public static String urlConPagina(String rutaBase, Map<String, List<String>> queryParams, int nuevaPagina) {
        StringBuilder sb = new StringBuilder(rutaBase);
        boolean primerParam = true;

        if (queryParams != null) {
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                String clave = entry.getKey();
                if ("pagina".equalsIgnoreCase(clave) || PARAMETROS_EFIMEROS.contains(clave)) {
                    continue;
                }
                for (String valor : entry.getValue()) {
                    if (valor == null || valor.isBlank()) continue;
                    sb.append(primerParam ? "?" : "&");
                    primerParam = false;
                    sb.append(URLEncoder.encode(clave, StandardCharsets.UTF_8))
                      .append("=")
                      .append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
                }
            }
        }

        if (nuevaPagina > 1) {
            sb.append(primerParam ? "?" : "&");
            sb.append("pagina=").append(nuevaPagina);
        }

        return sb.toString();
    }

    /**
     * Obtiene el indice de pagina para el backend (0-indexed) a partir del parametro "pagina" (1-indexed).
     */
    public static int paginaBackend(Context ctx) {
        return Math.max(0, entero(ctx.queryParam("pagina"), 1) - 1);
    }

    public static int entero(String valor, int porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
