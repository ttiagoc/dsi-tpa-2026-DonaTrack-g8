package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.SituacionDonacion;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.fotos.AlmacenFotos;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

/**
 * Req. 12: la entidad confirma que recibio una donacion entregada y carga fotos de lo que llego.
 * Las fotos se validan y se guardan en el disco (AlmacenFotos); a la API le llegan sus URLs.
 * Si algo falla, la pagina vuelve con el error y hay que elegir las fotos de nuevo (el navegador no las conserva).
 */
@Component
public class RecepcionController {

    private static final String VISTA = "entidad/recepcion.html.hbs";
    private static final String LISTADO = "/entidad/donaciones";
    private static final DateTimeFormatter ENTREGADA = DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm");

    private final DonacionesApiClient donaciones;
    private final AlmacenFotos almacen;

    public RecepcionController(DonacionesApiClient donaciones, AlmacenFotos almacen) {
        this.donaciones = donaciones;
        this.almacen = almacen;
    }

    public void mostrar(Context ctx) {
        DonacionResponse donacion = porConfirmar(ctx);
        if (donacion != null) {
            mostrar(ctx, donacion, Modelo.base(ctx));
        }
    }

    public void confirmar(Context ctx) {
        DonacionResponse donacion = porConfirmar(ctx);
        if (donacion == null) {
            return;
        }
        List<UploadedFile> fotos;
        try {
            // un input sin archivos manda una parte vacia
            fotos = ctx.uploadedFiles("fotos").stream().filter(f -> f.size() > 0).toList();
        } catch (RuntimeException e) {
            mostrarConError(ctx, donacion, "Cada foto puede pesar hasta " + AlmacenFotos.MAXIMO_MB + " MB");
            return;
        }
        String error = validar(fotos);
        if (error != null) {
            mostrarConError(ctx, donacion, error);
            return;
        }

        List<String> urls = new ArrayList<>();
        try {
            fotos.forEach(foto -> urls.add(almacen.guardar(foto)));
            donaciones.confirmarRecepcion(ctx, donacion.id(), urls);
        } catch (ApiException e) {
            almacen.borrar(urls);
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            mostrarConError(ctx, donacion, e.getMessage());
            return;
        } catch (RuntimeException e) {
            almacen.borrar(urls);
            throw e;
        }
        Alertas.exito(ctx, "Recepción de la donación #" + donacion.id() + " confirmada. ¡Gracias! El donante ya puede ver las fotos.");
        ctx.redirect(LISTADO);
    }

    // ---- Internos ----

    /** La donacion del path si todavia falta confirmarla; si no, vuelve al listado con un aviso y devuelve null. */
    private DonacionResponse porConfirmar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        DonacionResponse donacion = donaciones.obtenerDonacion(ctx, id);
        SituacionDonacion situacion = SituacionDonacion.de(donacion);
        if (situacion == SituacionDonacion.PARA_CONFIRMAR) {
            return donacion;
        }
        Alertas.info(ctx, situacion == SituacionDonacion.RECIBIDA
                ? "La recepción de la donación #" + id + " ya está confirmada."
                : "La donación #" + id + " todavía no fue entregada: vas a poder confirmarla cuando llegue.");
        ctx.redirect(LISTADO);
        return null;
    }

    private String validar(List<UploadedFile> fotos) {
        if (fotos.isEmpty()) {
            return "Cargá al menos una foto de lo que recibieron";
        }
        if (fotos.size() > AlmacenFotos.MAXIMO_FOTOS) {
            return "Podés cargar hasta " + AlmacenFotos.MAXIMO_FOTOS + " fotos";
        }
        return fotos.stream().map(almacen::validar).filter(e -> e != null).findFirst().orElse(null);
    }

    private void mostrarConError(Context ctx, DonacionResponse donacion, String error) {
        Map<String, Object> modelo = Modelo.base(ctx);
        modelo.put("errores", Map.of("fotos", error));
        modelo.put("error", "Revisá los datos marcados.");
        ctx.status(400);
        mostrar(ctx, donacion, modelo);
    }

    private void mostrar(Context ctx, DonacionResponse donacion, Map<String, Object> modelo) {
        String necesidad = donacion.necesidad() != null
                ? donacion.necesidad().subcategoria() + " · " + donacion.necesidad().descripcion()
                : donacion.subcategoria() + " (la necesidad se eliminó)";
        modelo.put("donacion", Map.of(
                "id", donacion.id(),
                "necesidad", necesidad,
                "bienes", DonacionesAsignadasController.DonacionVista.de(donacion).bienes(),
                "entregada", donacion.fechaEstado() == null ? "" : "El " + donacion.fechaEstado().format(ENTREGADA)));
        modelo.put("maximoFotos", AlmacenFotos.MAXIMO_FOTOS);
        modelo.put("maximoMb", AlmacenFotos.MAXIMO_MB);
        ctx.render(VISTA, modelo);
    }
}
