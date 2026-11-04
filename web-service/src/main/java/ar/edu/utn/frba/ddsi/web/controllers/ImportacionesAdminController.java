package ar.edu.utn.frba.ddsi.web.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.importacion.ImportacionResponse;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.springframework.stereotype.Component;

@Component
public class ImportacionesAdminController {

    private final DonacionesApiClient donaciones;

    public ImportacionesAdminController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        List<ImportacionResponse> importaciones = donaciones.listarImportaciones(ctx);

        String activaId = ctx.queryParam("activa");
        ImportacionResponse importacionActiva = null;

        if (activaId != null && !activaId.isBlank()) {
            try {
                importacionActiva = donaciones.obtenerImportacion(ctx, activaId);
            } catch (Exception ignored) {}
        }

        if (importacionActiva == null) {
            importacionActiva = importaciones.stream()
                    .filter(ImportacionResponse::esEnProceso)
                    .findFirst()
                    .orElse(null);
        }

        int totalImportaciones = importaciones.size();
        int totalDonantesProcesados = importaciones.stream()
                .mapToInt(ImportacionResponse::procesadas)
                .sum();
        long totalEnProceso = importaciones.stream()
                .filter(ImportacionResponse::esEnProceso)
                .count();

        Map<String, Object> resumen = Map.of(
                "total", totalImportaciones,
                "donantesProcesados", String.format("%,d", totalDonantesProcesados).replace(',', '.'),
                "enProceso", totalEnProceso
        );

        Map<String, Object> modelo = Modelo.base(ctx);
        modelo.put("importaciones", importaciones);
        modelo.put("tieneImportaciones", !importaciones.isEmpty());
        modelo.put("importacionActiva", importacionActiva != null ? importacionActiva : false);
        modelo.put("resumen", resumen);

        ctx.render("admin/importaciones.html.hbs", modelo);
    }

    public void importar(Context ctx) {
        UploadedFile archivo;
        try {
            archivo = ctx.uploadedFile("archivo");
        } catch (Exception e) {
            Alertas.error(ctx, "No se pudo leer el archivo cargado: " + e.getMessage());
            ctx.redirect("/admin/importaciones");
            return;
        }

        if (archivo == null || archivo.size() == 0) {
            Alertas.error(ctx, "Tenés que seleccionar un archivo CSV para importar.");
            ctx.redirect("/admin/importaciones");
            return;
        }

        String nombre = archivo.filename();
        if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
            Alertas.error(ctx, "El archivo cargado debe tener extensión .csv");
            ctx.redirect("/admin/importaciones");
            return;
        }

        byte[] bytes;
        try {
            bytes = archivo.content().readAllBytes();
        } catch (IOException e) {
            Alertas.error(ctx, "Error al procesar el archivo CSV: " + e.getMessage());
            ctx.redirect("/admin/importaciones");
            return;
        }

        try {
            ImportacionResponse resp = donaciones.iniciarImportacion(ctx, bytes, nombre);
            Alertas.exito(ctx, "Importación masiva iniciada (" + resp.id() + "). El procesamiento continuará en segundo plano.");
            ctx.redirect("/admin/importaciones?activa=" + resp.id());
        } catch (Exception e) {
            Alertas.error(ctx, "Error al iniciar la importación: " + e.getMessage());
            ctx.redirect("/admin/importaciones");
        }
    }

    public void consultarEstado(Context ctx) {
        String id = ctx.pathParam("id");
        ImportacionResponse resp = donaciones.obtenerImportacion(ctx, id);
        ctx.json(resp);
    }
}
