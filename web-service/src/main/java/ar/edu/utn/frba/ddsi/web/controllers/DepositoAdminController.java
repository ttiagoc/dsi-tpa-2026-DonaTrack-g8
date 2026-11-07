package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.comun.PaginaResponse;
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

        cargarDatosTab(ctx, modelo, filtro);

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

    public void reprogramar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.cambiarEstado(ctx, id, "ASIGNACION_REALIZADA", "Envío reprogramado por administración");
            Alertas.exito(ctx, "Se reprogramó el envío de la donación #" + id + ". Ya está lista para planificar en una nueva ruta.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo reprogramar la donación #" + id + ": " + e.getMessage());
        }
        ctx.redirect(RUTA + "?filtro=fallidas");
    }

    public void reingresar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.cambiarEstado(ctx, id, "EN_DEPOSITO", "Reingresada al stock de depósito tras entrega fallida");
            Alertas.exito(ctx, "La donación #" + id + " volvió al stock en depósito.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo devolver a depósito la donación #" + id + ": " + e.getMessage());
            ctx.redirect(RUTA + "?filtro=fallidas");
            return;
        }
        ctx.redirect(RUTA + "?filtro=en_deposito");
    }

    private void cargarDatosTab(Context ctx, Map<String, Object> modelo, String filtro) {
        List<DonacionResponse> enDeposito = donaciones.listarDonaciones(ctx, "EN_DEPOSITO");
        List<DonacionResponse> vencidas = donaciones.listarDonaciones(ctx, "VENCIDA");
        List<DonacionResponse> fallidas = donaciones.listarDonaciones(ctx, "ENTREGA_FALLIDA");
        Map<Long, String> nombresEntidades = obtenerMapaEntidades(ctx);

        long totalUnidades = enDeposito.stream().mapToLong(DonacionResponse::totalUnidades).sum();
        modelo.put("resumen", Map.of(
                "enDeposito", enDeposito.size(),
                "unidades", totalUnidades,
                "vencidas", vencidas.size(),
                "fallidas", fallidas.size()
        ));

        boolean esVencidas = "vencidas".equalsIgnoreCase(filtro);
        boolean esFallidas = "fallidas".equalsIgnoreCase(filtro);
        boolean esEnDeposito = !esVencidas && !esFallidas;

        modelo.put("esTabEnDeposito", esEnDeposito);
        modelo.put("esTabVencidas", esVencidas);
        modelo.put("esTabFallidas", esFallidas);

        List<DonacionResponse> fuente = esFallidas ? fallidas : (esVencidas ? vencidas : enDeposito);
        List<DonacionDepositoFila> filas = fuente.stream()
                .map(d -> new DonacionDepositoFila(
                        d.id(),
                        d.subcategoria(),
                        d.categoria(),
                        d.bienesResumen(),
                        d.fechaFormateada(),
                        d.esVencida(),
                        d.esEntregaFallida(),
                        d.justificacion(),
                        d.entidadAsignadaId() != null ? nombresEntidades.get(d.entidadAsignadaId()) : null))
                .toList();

        PaginaResponse<DonacionDepositoFila> pagina = Paginador.paginarLista(ctx, filas, 10);
        modelo.put("donaciones", pagina.elementos());
        modelo.put("paginacion", Paginador.armarModelo(ctx, pagina, RUTA));
    }

    private Map<Long, String> obtenerMapaEntidades(Context ctx) {
        try {
            Map<Long, String> mapa = new LinkedHashMap<>();
            for (Map<String, Object> e : donaciones.obtenerEntidades(ctx)) {
                Object idVal = e.get("id");
                Object nombreVal = e.get("razonSocial") != null ? e.get("razonSocial") : e.get("nombre");
                if (idVal instanceof Number n && nombreVal != null) {
                    mapa.put(n.longValue(), nombreVal.toString());
                }
            }
            return mapa;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void renderizarConError(Context ctx, Long id, Formulario form, String mensajeError) {
        Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
        modelo.put("errorGeneral", mensajeError);
        String filtro = "en_deposito";
        modelo.put("filtro", filtro);

        cargarDatosTab(ctx, modelo, filtro);

        try {
            DonacionResponse donacion = donaciones.obtenerDonacion(ctx, id);
            modelo.put("abrirModalVencer", true);
            modelo.put("donacionAVencer", donacion);
        } catch (Exception ignored) {
        }

        ctx.render("admin/deposito.html.hbs", modelo);
    }

    public record DonacionDepositoFila(
            Long id,
            String subcategoria,
            String categoria,
            String bienesResumen,
            String fechaFormateada,
            boolean esVencida,
            boolean esEntregaFallida,
            String justificacion,
            String entidadAsignadaNombre) {
    }
}
