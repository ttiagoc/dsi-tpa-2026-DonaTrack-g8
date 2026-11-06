package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient.ParadaDto;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient.RutaDto;
import ar.edu.utn.frba.ddsi.web.dto.camiones.CamionResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.exceptions.SesionRequeridaException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.http.Context;

@Component
public class ChoferWebController {

    private static final String VISTA = "chofer/hoja-de-ruta.html.hbs";
    private static final String RUTA = "/chofer";
    private static final double DEPOSITO_LAT = -34.5986;
    private static final double DEPOSITO_LNG = -58.4202;

    private final LogisticaApiClient logistica;
    private final DonacionesApiClient donaciones;

    public ChoferWebController(LogisticaApiClient logistica, DonacionesApiClient donaciones) {
        this.logistica = logistica;
        this.donaciones = donaciones;
    }

    /**
     * Hoja de ruta del chofer. Muestra todas las rutas de su camión (en traslado, planificadas y finalizadas)
     * y el detalle de la elegida con ?ruta={id}. Sin elegir, abre la que está en traslado o la próxima planificada.
     */
    public void hojaDeRuta(Context ctx) {
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);
        renderizarHojaDeRuta(ctx, modelo,
                parseLongOpcional(ctx.queryParam("ruta")),
                parseLongOpcional(ctx.queryParam("confirmarParada")),
                parseLongOpcional(ctx.queryParam("fallarParada")),
                parseLongOpcional(ctx.queryParam("completada")));
    }

    public void iniciarRuta(Context ctx) {
        Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
        // un camión hace un recorrido a la vez: la próxima ruta se empieza al terminar la que está en traslado
        RutaDto enTraslado = listarRutasSeguro(ctx, obtenerCamionId(ctx)).stream()
                .filter(RutaDto::esEnTraslado)
                .filter(r -> !rutaId.equals(r.id()))
                .findFirst()
                .orElse(null);
        if (enTraslado != null) {
            Alertas.error(ctx, "Terminá la Ruta #" + enTraslado.id() + " antes de comenzar otro recorrido.");
            ctx.redirect(RUTA + "?ruta=" + rutaId);
            return;
        }
        try {
            logistica.cambiarEstadoRuta(ctx, rutaId, "EN_TRASLADO");
            Alertas.exito(ctx, "Iniciaste el recorrido de la Ruta #" + rutaId + ". Tu ubicación GPS ya está activa.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo iniciar el recorrido: " + e.getMessage());
        }
        ctx.redirect(RUTA + "?ruta=" + rutaId);
    }

    public void confirmarParada(Context ctx) {
        Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
        Long paradaId = Long.parseLong(ctx.pathParam("paradaId"));
        try {
            logistica.confirmarParada(ctx, rutaId, paradaId);
            if (rutaFinalizada(ctx, rutaId)) {
                ctx.redirect(RUTA + "?completada=" + rutaId);
                return;
            }
            Alertas.exito(ctx, "Entrega confirmada en destino. Se actualizó tu posición GPS y se notificó a la entidad.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo confirmar la entrega: " + e.getMessage());
        }
        ctx.redirect(RUTA + "?ruta=" + rutaId);
    }

    public void fallarParada(Context ctx) {
        Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
        Long paradaId = Long.parseLong(ctx.pathParam("paradaId"));
        Formulario form = new Formulario(ctx);
        String motivo = form.requerido("motivo", "El motivo");

        if (form.tieneErrores()) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", "Indicá el motivo por el que no se pudo realizar la entrega.");
            renderizarHojaDeRuta(ctx, modelo, rutaId, null, paradaId, null);
            return;
        }

        try {
            logistica.reportarFalloParada(ctx, rutaId, paradaId, motivo);
            if (rutaFinalizada(ctx, rutaId)) {
                ctx.redirect(RUTA + "?completada=" + rutaId);
                return;
            }
            Alertas.info(ctx, "Se registró la entrega fallida y las donaciones quedaron informadas para reprogramación en depósito.");
            ctx.redirect(RUTA + "?ruta=" + rutaId);
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", e.getMessage());
            renderizarHojaDeRuta(ctx, modelo, rutaId, null, paradaId, null);
        }
    }

    private boolean rutaFinalizada(Context ctx, Long rutaId) {
        return listarRutasSeguro(ctx, obtenerCamionId(ctx)).stream()
                .filter(r -> rutaId.equals(r.id()))
                .findFirst()
                .map(RutaDto::esFinalizada)
                .orElse(true);
    }

    /** En traslado primero, después las planificadas de la más próxima a la más lejana y al final las finalizadas. */
    private static final Comparator<RutaDto> ORDEN_RUTAS = Comparator
            .comparingInt((RutaDto r) -> r.esEnTraslado() ? 0 : r.esPlanificada() ? 1 : 2)
            .thenComparing((a, b) -> {
                if (a.fecha() == null || b.fecha() == null) {
                    return 0;
                }
                return a.esFinalizada() ? b.fecha().compareTo(a.fecha()) : a.fecha().compareTo(b.fecha());
            })
            .thenComparing(RutaDto::id, Comparator.nullsLast(Comparator.reverseOrder()));

    private void renderizarHojaDeRuta(
            Context ctx,
            Map<String, Object> modelo,
            Long rutaIdElegida,
            Long confirmarParadaId,
            Long fallarParadaId,
            Long completadaId) {
        Long camionId = obtenerCamionId(ctx);
        CamionResponse camion = obtenerCamionSeguro(ctx, camionId);
        modelo.put("camion", camion);
        if (camion != null) {
            modelo.put("cargaCamionFormateada", camion.capacidadCarga() != null
                    ? String.format(Locale.US, "%.0f", camion.capacidadCarga()) : "-");
            modelo.put("volumenCamionFormateado", camion.capacidadVolumen() != null
                    ? String.format(Locale.US, "%.1f", camion.capacidadVolumen()) : "-");
        }
        modelo.put("depositoLat", String.format(Locale.US, "%.6f", DEPOSITO_LAT));
        modelo.put("depositoLng", String.format(Locale.US, "%.6f", DEPOSITO_LNG));

        List<RutaDto> rutas = new ArrayList<>(listarRutasSeguro(ctx, camionId));
        rutas.sort(ORDEN_RUTAS);

        if (completadaId != null) {
            rutas.stream()
                    .filter(r -> completadaId.equals(r.id()) && r.esFinalizada())
                    .findFirst()
                    .ifPresent(r -> {
                        modelo.put("abrirModalCompletada", true);
                        modelo.put("rutaCompletada", armarVistaCompletada(r, camion));
                    });
        }

        // la elegida (si es de este camión) o la primera sin terminar
        RutaDto ruta = rutas.stream().filter(r -> r.id() != null && r.id().equals(rutaIdElegida)).findFirst()
                .orElse(rutas.stream().filter(r -> !r.esFinalizada()).findFirst().orElse(null));
        RutaDto enTraslado = rutas.stream().filter(RutaDto::esEnTraslado).findFirst().orElse(null);

        List<RutaOpcionVista> activas = new ArrayList<>();
        List<RutaOpcionVista> finalizadas = new ArrayList<>();
        for (RutaDto r : rutas) {
            RutaOpcionVista opcion = new RutaOpcionVista(r.id(), r.fechaFormateada(), r.estado(), r.paradasTexto(),
                    textoPendientes(contarPendientes(r)), ruta != null && r.id() != null && r.id().equals(ruta.id()));
            (r.esFinalizada() ? finalizadas : activas).add(opcion);
        }
        modelo.put("rutasActivas", activas);
        modelo.put("rutasFinalizadas", finalizadas);
        modelo.put("rutasActivasCount", activas.size());
        modelo.put("rutasFinalizadasCount", finalizadas.size());
        modelo.put("hayVariasRutas", rutas.size() > 1);
        modelo.put("rutasActivasTexto", activas.size() == 1 ? "1 ruta por hacer" : activas.size() + " rutas por hacer");

        if (ruta == null) {
            ctx.render(VISTA, modelo);
            return;
        }

        modelo.put("ruta", ruta);
        modelo.put("esPlanificada", ruta.esPlanificada());
        modelo.put("esEnTraslado", ruta.esEnTraslado());
        modelo.put("esFinalizada", ruta.esFinalizada());
        if (ruta.esPlanificada() && enTraslado != null) {
            modelo.put("otraEnTraslado", enTraslado);
        }

        ParadaDto ultimaResuelta = null;
        if (ruta.paradas() != null) {
            ultimaResuelta = ruta.paradas().stream()
                    .filter(p -> "ENTREGADA".equalsIgnoreCase(p.estado()) || "NO_RECIBIDA".equalsIgnoreCase(p.estado()))
                    .max(Comparator.comparing(ParadaDto::orden, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
        }

        double camionLat;
        double camionLng;
        if (ultimaResuelta != null && ultimaResuelta.latitud() != null && ultimaResuelta.longitud() != null) {
            camionLat = ultimaResuelta.latitud();
            camionLng = ultimaResuelta.longitud();
        } else if (ultimaResuelta != null && ruta.camionLatitud() != null && ruta.camionLongitud() != null) {
            camionLat = ruta.camionLatitud();
            camionLng = ruta.camionLongitud();
        } else {
            // Posición inicial: Depósito central antes de realizar entregas
            camionLat = DEPOSITO_LAT;
            camionLng = DEPOSITO_LNG;
        }
        modelo.put("camionLat", String.format(Locale.US, "%.6f", camionLat));
        modelo.put("camionLng", String.format(Locale.US, "%.6f", camionLng));

        Map<Long, String> entidades = obtenerMapaEntidades(ctx);
        Map<Long, DonacionResponse> cacheDonaciones = new LinkedHashMap<>();
        List<ParadaChoferVista> paradas = new ArrayList<>();
        boolean proximaAsignada = false;
        int pendientesCount = 0;

        if (ruta.paradas() != null) {
            List<ParadaDto> ordenadas = new ArrayList<>(ruta.paradas());
            ordenadas.sort(Comparator.comparing(ParadaDto::orden, Comparator.nullsLast(Comparator.naturalOrder())));

            for (ParadaDto p : ordenadas) {
                String estado = p.estado() != null ? p.estado() : "PENDIENTE";
                boolean esPendiente = "PENDIENTE".equalsIgnoreCase(estado);
                boolean esEntregada = "ENTREGADA".equalsIgnoreCase(estado);
                boolean esFallida = "NO_RECIBIDA".equalsIgnoreCase(estado);
                boolean esProxima = false;
                if (esPendiente) {
                    pendientesCount++;
                    if (!proximaAsignada) {
                        esProxima = true;
                        proximaAsignada = true;
                    }
                }

                String entidadNombre = p.entidad() != null && entidades.containsKey(p.entidad())
                        ? entidades.get(p.entidad())
                        : (p.entidad() != null ? "Entidad #" + p.entidad() : "Sede beneficiaria");
                entidadNombre = limpiarLinea(entidadNombre);
                String destinoLimpio = limpiarLinea(p.destino() != null ? p.destino() : "Sin dirección informada");

                List<String> bultos = new ArrayList<>();
                List<String> bultosDetalle = new ArrayList<>();
                if (p.entregas() != null) {
                    for (Long donId : p.entregas()) {
                        if (donId == null) continue;
                        DonacionResponse d = cacheDonaciones.computeIfAbsent(donId, id -> obtenerDonacionSegura(ctx, id));
                        if (d != null && d.subcategoria() != null) {
                            bultos.add("Donación #" + donId + " · " + d.subcategoria() + " (" + d.totalUnidades() + " u)");
                            bultosDetalle.add("Donación #" + donId + " · " + d.subcategoria() + " (" + d.bienesResumen() + ")");
                        } else {
                            bultos.add("Donación #" + donId);
                            bultosDetalle.add("Donación #" + donId);
                        }
                    }
                }

                // sin coordenadas la parada se lista pero no se marca en el mapa (antes caía en un punto inventado)
                ParadaChoferVista vista = new ParadaChoferVista(
                        p.id(),
                        p.orden() != null ? p.orden() : paradas.size() + 1,
                        entidadNombre,
                        destinoLimpio,
                        estado,
                        esPendiente,
                        esProxima,
                        esEntregada,
                        esFallida,
                        bultos,
                        bultosDetalle.isEmpty() ? "Sin bultos informados" : String.join(" · ", bultosDetalle),
                        p.latitud() != null ? String.format(Locale.US, "%.6f", p.latitud()) : null,
                        p.longitud() != null ? String.format(Locale.US, "%.6f", p.longitud()) : null);
                paradas.add(vista);

                if (ruta.esEnTraslado() && esPendiente) {
                    if (confirmarParadaId != null && confirmarParadaId.equals(p.id())) {
                        modelo.put("abrirModalConfirmar", true);
                        modelo.put("paradaSeleccionada", vista);
                    }
                    if (fallarParadaId != null && fallarParadaId.equals(p.id())) {
                        modelo.put("abrirModalFallo", true);
                        modelo.put("paradaSeleccionada", vista);
                    }
                }
            }
        }

        modelo.put("paradas", paradas);
        modelo.put("pendientesTexto", textoPendientes(pendientesCount));
        modelo.put("ordenResumen", paradas.stream()
                .map(p -> String.valueOf(p.orden()))
                .collect(Collectors.joining(" → ")));

        ctx.render(VISTA, modelo);
    }

    private static int contarPendientes(RutaDto ruta) {
        if (ruta.paradas() == null) {
            return 0;
        }
        return (int) ruta.paradas().stream()
                .filter(p -> p.estado() == null || "PENDIENTE".equalsIgnoreCase(p.estado()))
                .count();
    }

    private static String textoPendientes(int pendientes) {
        return pendientes == 1 ? "1 parada pendiente" : pendientes + " paradas pendientes";
    }

    private Long obtenerCamionId(Context ctx) {
        Long perfilId = SesionUsuario.actual(ctx)
                .orElseThrow(SesionRequeridaException::new)
                .getUsuario()
                .perfilId();
        if (perfilId == null) {
            throw new SesionRequeridaException();
        }
        return perfilId;
    }

    private CamionResponse obtenerCamionSeguro(Context ctx, Long camionId) {
        try {
            return logistica.obtenerCamion(ctx, camionId);
        } catch (Exception e) {
            return null;
        }
    }

    /** Todas las rutas del camión (la API valida que sea el del chofer logueado). */
    private List<RutaDto> listarRutasSeguro(Context ctx, Long camionId) {
        try {
            List<RutaDto> rutas = logistica.listarRutasCamion(ctx, camionId);
            return rutas != null ? rutas : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private DonacionResponse obtenerDonacionSegura(Context ctx, Long id) {
        try {
            return donaciones.obtenerDonacion(ctx, id);
        } catch (Exception e) {
            return null;
        }
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
        } catch (Exception e) {
            return Map.of();
        }
    }

    private RutaCompletadaVista armarVistaCompletada(RutaDto ruta, CamionResponse camion) {
        List<ParadaDto> paradas = ruta.paradas() != null ? ruta.paradas() : List.of();
        int total = paradas.size();
        long entregadas = paradas.stream()
                .filter(p -> "ENTREGADA".equalsIgnoreCase(p.estado()))
                .count();
        long fallidas = paradas.stream()
                .filter(p -> "NO_RECIBIDA".equalsIgnoreCase(p.estado()))
                .count();
        boolean todasEntregadas = fallidas == 0;

        String resumenParadas;
        if (total == 0) {
            resumenParadas = "Recorrido finalizado";
        } else if (fallidas == 0) {
            resumenParadas = total == 1
                    ? "1 parada entregada"
                    : total + " paradas resueltas (" + entregadas + " entregadas)";
        } else if (entregadas == 0) {
            resumenParadas = total == 1
                    ? "1 parada no recibida"
                    : total + " paradas resueltas (" + fallidas + " no recibidas)";
        } else {
            resumenParadas = total + " paradas resueltas ("
                    + entregadas + (entregadas == 1 ? " entregada · " : " entregadas · ")
                    + fallidas + (fallidas == 1 ? " no recibida)" : " no recibidas)");
        }

        String camionDetalle = null;
        if (camion != null && camion.patente() != null) {
            camionDetalle = camion.patente() + " (" + camion.choferNombreCompleto() + ")";
        } else if (ruta.patenteCamion() != null) {
            camionDetalle = ruta.patenteCamion() + " (" + ruta.choferNombreCompleto() + ")";
        }

        return new RutaCompletadaVista(ruta.id(), camionDetalle, resumenParadas, todasEntregadas);
    }

    private String limpiarLinea(String texto) {
        return texto == null ? "" : texto.replace("\r", " ").replace("\n", " ").trim();
    }

    private Long parseLongOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Una ruta del selector de la hoja de ruta. */
    public record RutaOpcionVista(
            Long id,
            String fecha,
            String estado,
            String paradasTexto,
            String pendientesTexto,
            boolean elegida) {
    }

    public record RutaCompletadaVista(
            Long id,
            String camionDetalle,
            String resumenParadas,
            boolean todasEntregadas) {
    }

    public record ParadaChoferVista(
            Long id,
            Integer orden,
            String entidadNombre,
            String destino,
            String estado,
            boolean esPendiente,
            boolean esProxima,
            boolean esEntregada,
            boolean esFallida,
            List<String> bultos,
            String bultosResumen,
            String latitud,
            String longitud) {
    }
}
