package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaConSubcategorias;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaRequest;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.CategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.SubcategoriaResponse;
import ar.edu.utn.frba.ddsi.web.dto.catalogo.UsosCatalogoResponse;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

/**
 * ABM del catalogo (/admin/categorias): categorias y sus subcategorias.
 * La baja solo se permite si ninguna donacion ni necesidad las usa; borrar una categoria borra sus subcategorias.
 * Los modales se abren con ?nueva, ?editar={id}, ?nuevaSub={categoriaId}, ?editarSub={subcategoriaId},
 * ?eliminar={id} y ?eliminarSub={subcategoriaId}.
 */
@Component
public class CategoriasAdminController {

    private static final String VISTA = "admin/categorias.html.hbs";
    private static final String RUTA = "/admin/categorias";

    private final DonacionesApiClient donaciones;

    public CategoriasAdminController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        List<CategoriaConSubcategorias> catalogo = donaciones.catalogoCompleto(ctx);
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);

        Long editar = idDe(ctx.queryParam("editar"));
        Long nuevaSub = idDe(ctx.queryParam("nuevaSub"));
        Long editarSub = idDe(ctx.queryParam("editarSub"));
        Long eliminar = idDe(ctx.queryParam("eliminar"));
        Long eliminarSub = idDe(ctx.queryParam("eliminarSub"));

        if (ctx.queryParam("nueva") != null) {
            modelo.put("abrirCategoria", true);
        } else if (editar != null) {
            Optional<CategoriaResponse> categoria = buscarCategoria(catalogo, editar);
            if (categoria.isPresent()) {
                modelo.put("abrirCategoria", true);
                modelo.put("categoriaId", editar);
                modelo.put("form", valoresCategoria(categoria.get()));
            } else {
                Alertas.error(ctx, "No se encontró la categoría a editar.");
            }
        } else if (nuevaSub != null) {
            Optional<CategoriaResponse> categoria = buscarCategoria(catalogo, nuevaSub);
            if (categoria.isPresent()) {
                modelo.put("abrirSubcategoria", true);
                modelo.put("categoriaPadre", categoria.get());
            } else {
                Alertas.error(ctx, "No se encontró la categoría.");
            }
        } else if (editarSub != null) {
            Optional<CategoriaConSubcategorias> fila = catalogo.stream()
                    .filter(f -> f.subcategorias().stream().anyMatch(s -> s.id().equals(editarSub)))
                    .findFirst();
            if (fila.isPresent()) {
                SubcategoriaResponse sub = fila.get().subcategorias().stream()
                        .filter(s -> s.id().equals(editarSub)).findFirst().orElseThrow();
                modelo.put("abrirSubcategoria", true);
                modelo.put("categoriaPadre", fila.get().categoria());
                modelo.put("subcategoriaId", editarSub);
                modelo.put("form", Map.of("subcategoria", sub.nombre()));
            } else {
                Alertas.error(ctx, "No se encontró la subcategoría a editar.");
            }
        } else if (eliminar != null) {
            Optional<CategoriaConSubcategorias> fila = catalogo.stream()
                    .filter(f -> f.categoria().id().equals(eliminar)).findFirst();
            if (fila.isPresent()) {
                Map<String, Object> baja = baja("categoría", fila.get().categoria().nombre(),
                        RUTA + "/" + eliminar + "/eliminar", donaciones.usosCategoria(ctx, eliminar));
                baja.put("subcategorias", fila.get().subcategorias().stream().map(SubcategoriaResponse::nombre).toList());
                modelo.put("baja", baja);
            } else {
                Alertas.error(ctx, "No se encontró la categoría a eliminar.");
            }
        } else if (eliminarSub != null) {
            Optional<SubcategoriaResponse> sub = catalogo.stream().flatMap(f -> f.subcategorias().stream())
                    .filter(s -> s.id().equals(eliminarSub)).findFirst();
            if (sub.isPresent()) {
                Map<String, Object> baja = baja("subcategoría", sub.get().nombre(),
                        "/admin/subcategorias/" + eliminarSub + "/eliminar", donaciones.usosSubcategoria(ctx, eliminarSub));
                baja.put("categoria", sub.get().categoria());
                modelo.put("baja", baja);
            } else {
                Alertas.error(ctx, "No se encontró la subcategoría a eliminar.");
            }
        }

        mostrar(ctx, modelo, catalogo);
    }

    public void eliminar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.eliminarCategoria(ctx, id);
            Alertas.exito(ctx, "Se eliminó la categoría con sus subcategorías.");
        } catch (ApiException e) {
            Alertas.error(ctx, e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    public void eliminarSubcategoria(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.eliminarSubcategoria(ctx, id);
            Alertas.exito(ctx, "Se eliminó la subcategoría.");
        } catch (ApiException e) {
            Alertas.error(ctx, e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    /** Datos del modal de baja: la confirmacion o, si esta en uso, por que no se puede. */
    private static Map<String, Object> baja(String tipo, String nombre, String action, UsosCatalogoResponse usos) {
        Map<String, Object> baja = new LinkedHashMap<>();
        baja.put("tipo", tipo);
        baja.put("nombre", nombre);
        baja.put("action", action);
        baja.put("enUso", usos.enUso());
        baja.put("donaciones", cantidad(usos.donaciones(), "donación", "donaciones"));
        baja.put("necesidades", cantidad(usos.necesidades(), "necesidad", "necesidades"));
        return baja;
    }

    /** "3 donaciones", "1 necesidad"; null si es 0 (asi la plantilla no lo muestra). */
    private static String cantidad(long n, String singular, String plural) {
        return n == 0 ? null : n + " " + (n == 1 ? singular : plural);
    }

    public void crear(Context ctx) {
        guardarCategoria(ctx, null);
    }

    public void actualizar(Context ctx) {
        guardarCategoria(ctx, Long.parseLong(ctx.pathParam("id")));
    }

    public void crearSubcategoria(Context ctx) {
        guardarSubcategoria(ctx, Long.parseLong(ctx.pathParam("id")), null);
    }

    public void actualizarSubcategoria(Context ctx) {
        guardarSubcategoria(ctx, null, Long.parseLong(ctx.pathParam("id")));
    }

    private void guardarCategoria(Context ctx, Long id) {
        Formulario form = new Formulario(ctx);
        String nombre = form.requerido("nombre", "El nombre");
        Boolean pideEstado = siNo(form, "pideEstado", "Indicá si se registra el estado del bien");
        Boolean esPerecedero = siNo(form, "esPerecedero", "Indicá si la categoría es perecedera");

        String error = form.tieneErrores() ? "Revisá los datos ingresados." : null;
        if (error == null) {
            CategoriaRequest request = new CategoriaRequest(nombre, pideEstado, esPerecedero);
            try {
                if (id == null) {
                    donaciones.crearCategoria(ctx, request);
                    Alertas.exito(ctx, "Se creó la categoría " + nombre + ".");
                } else {
                    donaciones.actualizarCategoria(ctx, id, request);
                    Alertas.exito(ctx, "Se actualizó la categoría " + nombre + ".");
                }
                ctx.redirect(RUTA);
                return;
            } catch (ApiException e) {
                error = e.getMessage();
            }
        }

        ctx.status(400);
        Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
        modelo.put("errorModal", error);
        modelo.put("abrirCategoria", true);
        modelo.put("categoriaId", id);
        mostrar(ctx, modelo, donaciones.catalogoCompleto(ctx));
    }

    /** categoriaId en el alta, subcategoriaId en la edicion. */
    private void guardarSubcategoria(Context ctx, Long categoriaId, Long subcategoriaId) {
        Formulario form = new Formulario(ctx);
        String nombre = form.requerido("subcategoria", "El nombre");

        String error = form.tieneErrores() ? "Revisá los datos ingresados." : null;
        if (error == null) {
            SubcategoriaRequest request = new SubcategoriaRequest(nombre);
            try {
                if (subcategoriaId == null) {
                    donaciones.crearSubcategoria(ctx, categoriaId, request);
                    Alertas.exito(ctx, "Se agregó la subcategoría " + nombre + ".");
                } else {
                    donaciones.actualizarSubcategoria(ctx, subcategoriaId, request);
                    Alertas.exito(ctx, "Se actualizó la subcategoría " + nombre + ".");
                }
                ctx.redirect(RUTA);
                return;
            } catch (ApiException e) {
                error = e.getMessage();
            }
        }

        ctx.status(400);
        List<CategoriaConSubcategorias> catalogo = donaciones.catalogoCompleto(ctx);
        Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
        modelo.put("errorModal", error);
        modelo.put("abrirSubcategoria", true);
        modelo.put("subcategoriaId", subcategoriaId);
        Optional<CategoriaResponse> padre = subcategoriaId == null
                ? buscarCategoria(catalogo, categoriaId)
                : catalogo.stream()
                        .filter(f -> f.subcategorias().stream().anyMatch(s -> s.id().equals(subcategoriaId)))
                        .map(CategoriaConSubcategorias::categoria)
                        .findFirst();
        padre.ifPresent(c -> modelo.put("categoriaPadre", c));
        mostrar(ctx, modelo, catalogo);
    }

    private void mostrar(Context ctx, Map<String, Object> modelo, List<CategoriaConSubcategorias> catalogo) {
        modelo.put("catalogo", catalogo);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("categorias", catalogo.size());
        resumen.put("subcategorias", catalogo.stream().mapToInt(f -> f.subcategorias().size()).sum());
        resumen.put("perecederas", catalogo.stream().filter(f -> Boolean.TRUE.equals(f.categoria().esPerecedero())).count());
        modelo.put("resumen", resumen);

        ctx.render(VISTA, modelo);
    }

    private static Optional<CategoriaResponse> buscarCategoria(List<CategoriaConSubcategorias> catalogo, Long id) {
        return catalogo.stream().map(CategoriaConSubcategorias::categoria)
                .filter(c -> c.id().equals(id)).findFirst();
    }

    private static Map<String, String> valoresCategoria(CategoriaResponse categoria) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("nombre", categoria.nombre());
        valores.put("pideEstado", Boolean.TRUE.equals(categoria.pideEstado()) ? "si" : "no");
        valores.put("esPerecedero", Boolean.TRUE.equals(categoria.esPerecedero()) ? "si" : "no");
        return valores;
    }

    private static Boolean siNo(Formulario form, String campo, String error) {
        String valor = form.valor(campo);
        if ("si".equals(valor)) {
            return true;
        }
        if ("no".equals(valor)) {
            return false;
        }
        form.error(campo, error);
        return null;
    }

    private static Long idDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
