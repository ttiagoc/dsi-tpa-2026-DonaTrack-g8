package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

@Component
public class DepositoAdminController {

    private static final String RUTA = "/admin/deposito";
    private final DonacionesApiClient donaciones;

    public DepositoAdminController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);
        String filtro = ctx.queryParam("filtro");
        if (filtro == null || filtro.isBlank()) {
            filtro = "en_deposito";
        }
        modelo.put("filtro", filtro);

        List<DonacionResponse> enDeposito = donaciones.listarDonaciones(ctx, "EN_DEPOSITO");
        List<DonacionResponse> vencidas = donaciones.listarDonaciones(ctx, "VENCIDA");

        long totalUnidades = enDeposito.stream().mapToLong(DonacionResponse::totalUnidades).sum();
        modelo.put("resumen", Map.of(
                "enDeposito", enDeposito.size(),
                "unidades", totalUnidades,
                "vencidas", vencidas.size()
        ));

        boolean esVencidas = "vencidas".equalsIgnoreCase(filtro);
        modelo.put("esTabVencidas", esVencidas);
        modelo.put("esTabEnDeposito", !esVencidas);
        modelo.put("donaciones", esVencidas ? vencidas : enDeposito);

        String vencerParam = ctx.queryParam("vencer");
        if (vencerParam != null && !vencerParam.isBlank()) {
            try {
                Long vencerId = Long.parseLong(vencerParam);
                DonacionResponse donacion = donaciones.obtenerDonacion(ctx, vencerId);
                modelo.put("abrirModalVencer", true);
                modelo.put("donacionAVencer", donacion);
            } catch (Exception e) {
                Alertas.error(ctx, "No se encontró la donación para marcar como vencida.");
            }
        }

        ctx.render("admin/deposito.html.hbs", modelo);
    }

    public void marcarVencida(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Formulario form = new Formulario(ctx);
        String justificacion = form.requerido("justificacion", "El motivo o justificación");

        if (form.tieneErrores()) {
            ctx.status(400);
            renderizarConError(ctx, id, form, "Revisá los datos ingresados.");
            return;
        }

        try {
            donaciones.cambiarEstado(ctx, id, "VENCIDA", justificacion);
            Alertas.exito(ctx, "La donación #" + id + " fue marcada como vencida correctamente.");
            ctx.redirect(RUTA + "?filtro=vencidas");
        } catch (ApiException e) {
            ctx.status(400);
            renderizarConError(ctx, id, form, e.getMessage());
        }
    }

    private void renderizarConError(Context ctx, Long id, Formulario form, String mensajeError) {
        Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), mensajeError);
        String filtro = "en_deposito";
        modelo.put("filtro", filtro);

        List<DonacionResponse> enDeposito = donaciones.listarDonaciones(ctx, "EN_DEPOSITO");
        List<DonacionResponse> vencidas = donaciones.listarDonaciones(ctx, "VENCIDA");
        long totalUnidades = enDeposito.stream().mapToLong(DonacionResponse::totalUnidades).sum();
        modelo.put("resumen", Map.of(
                "enDeposito", enDeposito.size(),
                "unidades", totalUnidades,
                "vencidas", vencidas.size()
        ));
        modelo.put("esTabEnDeposito", true);
        modelo.put("esTabVencidas", false);
        modelo.put("donaciones", enDeposito);

        try {
            DonacionResponse donacion = donaciones.obtenerDonacion(ctx, id);
            modelo.put("abrirModalVencer", true);
            modelo.put("donacionAVencer", donacion);
        } catch (Exception ignored) {
        }

        ctx.render("admin/deposito.html.hbs", modelo);
    }
}
