package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.necesidades.Frecuencia;
import ar.edu.utn.frba.ddsi.web.dto.necesidades.NecesidadRequest;
import ar.edu.utn.frba.ddsi.web.dto.necesidades.NecesidadResponse;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

/**
 * Req. 10: la entidad carga y da de baja sus necesidades. Listado con el alta en un modal (?nueva lo abre sin JS)
 * y confirmacion antes de eliminar. Si el alta falla, se vuelve a mostrar el listado con el modal abierto.
 */
@Component
public class NecesidadesController {

    private static final String VISTA = "entidad/necesidades.html.hbs";
    private static final String RUTA = "/entidad/necesidades";

    private final DonacionesApiClient donaciones;

    public NecesidadesController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);
        modelo.put("abrirAlta", ctx.queryParam("nueva") != null);
        mostrar(ctx, modelo);
    }

    public void crear(Context ctx) {
        Formulario form = new Formulario(ctx);
        String subcategoria = form.requerido("subcategoria", "La subcategoría");
        Optional<Frecuencia> frecuencia = Frecuencia.deFormulario(form.valor("frecuencia"));
        if (frecuencia.isEmpty()) {
            form.error("frecuencia", "Elegí cada cuánto la necesitan");
        }
        Long cantidad = leerCantidad(form);
        String descripcion = form.requerido("descripcion", "La descripción");

        if (form.tieneErrores()) {
            mostrarAltaConError(ctx, form, "Revisá los datos marcados.");
            return;
        }
        try {
            donaciones.registrarNecesidad(ctx, new NecesidadRequest(new NecesidadRequest.Subcategoria(subcategoria),
                    frecuencia.get().tipoNecesidad(), frecuencia.get().periodo(), descripcion, cantidad));
            Alertas.exito(ctx, "Necesidad agregada.");
            ctx.redirect(RUTA);
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            mostrarAltaConError(ctx, form, e.getMessage());
        }
    }

    public void eliminar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.eliminarNecesidad(ctx, id);
            Alertas.exito(ctx, "Necesidad eliminada.");
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            Alertas.error(ctx, "No se pudo eliminar la necesidad: " + e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    // ---- Internos ----

    private Long leerCantidad(Formulario form) {
        String valor = form.requerido("cantidad", "La cantidad");
        if (valor == null) {
            return null;
        }
        try {
            long cantidad = Long.parseLong(valor);
            if (cantidad > 0) {
                return cantidad;
            }
        } catch (NumberFormatException e) {
            // cae al error de abajo
        }
        form.error("cantidad", "Tiene que ser un número mayor a 0");
        return null;
    }

    private void mostrarAltaConError(Context ctx, Formulario form, String error) {
        Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
        modelo.put("abrirAlta", true);
        modelo.put("errorAlta", error);
        ctx.status(400);
        mostrar(ctx, modelo);
    }

    private void mostrar(Context ctx, Map<String, Object> modelo) {
        List<NecesidadVista> necesidades = donaciones.listarNecesidades(ctx).stream().map(NecesidadVista::de).toList();
        long cubiertas = necesidades.stream().filter(NecesidadVista::satisfecha).count();
        modelo.put("necesidades", necesidades);
        modelo.put("resumen", Map.of("total", necesidades.size(), "cubiertas", cubiertas,
                "pendientes", necesidades.size() - cubiertas));
        modelo.put("rubros", donaciones.catalogo());
        modelo.put("frecuencias", Arrays.stream(Frecuencia.values())
                .map(f -> Map.of("valor", f.name(), "etiqueta", f.etiqueta())).toList());
        ctx.render(VISTA, modelo);
    }

    /** Una fila de la tabla, con la frecuencia en palabras y el porcentaje para la barra de progreso. */
    public record NecesidadVista(Long id, String subcategoria, String categoria, String descripcion,
            String frecuencia, Long cantidad, Long cantidadAsignada, int porcentaje, boolean satisfecha) {

        static NecesidadVista de(NecesidadResponse n) {
            long asignada = n.cantidadAsignada() == null ? 0 : n.cantidadAsignada();
            int porcentaje = n.cantidad() == null || n.cantidad() == 0 ? 0
                    : (int) Math.min(100, asignada * 100 / n.cantidad());
            return new NecesidadVista(n.id(), n.subcategoria(), n.categoria(), n.descripcion(),
                    Frecuencia.de(n).etiqueta(), n.cantidad(), asignada, porcentaje, Boolean.TRUE.equals(n.satisfecha()));
        }
    }
}
