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

    public void hojaDeRuta(Context ctx) {
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);
        Long confirmarId = parseLongOpcional(ctx.queryParam("confirmarParada"));
        Long fallarId = parseLongOpcional(ctx.queryParam("fallarParada"));
        Long completadaId = parseLongOpcional(ctx.queryParam("completada"));
        renderizarHojaDeRuta(ctx, modelo, confirmarId, fallarId, completadaId);
    }

    public void iniciarRuta(Context ctx) {
        Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
        try {
            logistica.cambiarEstadoRuta(ctx, rutaId, "EN_TRASLADO");
            Alertas.exito(ctx, "Iniciaste el recorrido de la Ruta #" + rutaId + ". Tu ubicación GPS ya está activa.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo iniciar el recorrido: " + e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    public void confirmarParada(Context ctx) {
        Long rutaId = Long.parseLong(ctx.pathParam("rutaId"));
        Long paradaId = Long.parseLong(ctx.pathParam("paradaId"));
        try {
            logistica.confirmarParada(ctx, rutaId, paradaId);
            Long camionId = obtenerCamionId(ctx);
            RutaDto sigueActiva = obtenerRutaActivaSegura(ctx, camionId);
            if (sigueActiva == null || !rutaId.equals(sigueActiva.id())) {
                ctx.redirect(RUTA + "?completada=" + rutaId);
                return;
            }
            Alertas.exito(ctx, "Entrega confirmada en destino. Se actualizó tu posición GPS y se notificó a la entidad.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo confirmar la entrega: " + e.getMessage());
        }
        ctx.redirect(RUTA);
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
            renderizarHojaDeRuta(ctx, modelo, null, paradaId, null);
            return;
        }

        try {
            logistica.reportarFalloParada(ctx, rutaId, paradaId, motivo);
            Long camionId = obtenerCamionId(ctx);
            RutaDto sigueActiva = obtenerRutaActivaSegura(ctx, camionId);
            if (sigueActiva == null || !rutaId.equals(sigueActiva.id())) {
                ctx.redirect(RUTA + "?completada=" + rutaId);
                return;
            }
            Alertas.info(ctx, "Se registró la entrega fallida y las donaciones quedaron informadas para reprogramación en depósito.");
            ctx.redirect(RUTA);
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", e.getMessage());
            renderizarHojaDeRuta(ctx, modelo, null, paradaId, null);
        }
    }

    private void renderizarHojaDeRuta(
            Context ctx,
            Map<String, Object> modelo,
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

        RutaDto ruta = obtenerRutaActivaSegura(ctx, camionId);
        modelo.put("depositoLat", String.format(Locale.US, "%.6f", DEPOSITO_LAT));
        modelo.put("depositoLng", String.format(Locale.US, "%.6f", DEPOSITO_LNG));

        List<RutaDto> historial = (ruta == null || completadaId != null)
                ? listarHistorialSeguro(ctx, camionId)
                : List.of();

        if (completadaId != null) {
            RutaDto rutaFinalizada = historial.stream()
                    .filter(r -> completadaId.equals(r.id()) && r.esFinalizada())
                    .findFirst()
                    .orElse(null);
            if (rutaFinalizada != null) {
                modelo.put("abrirModalCompletada", true);
                modelo.put("rutaCompletada", armarVistaCompletada(rutaFinalizada, camion));
            }
        }

        if (ruta == null) {
            long finalizadas = historial.stream().filter(RutaDto::esFinalizada).count();
            modelo.put("rutasFinalizadasCount", finalizadas);
            ctx.render(VISTA, modelo);
            return;
        }

        modelo.put("ruta", ruta);
        modelo.put("esPlanificada", ruta.esPlanificada());
        modelo.put("esEnTraslado", ruta.esEnTraslado());

        double camionLat = ruta.camionLatitud() != null ? ruta.camionLatitud() : DEPOSITO_LAT;
        double camionLng = ruta.camionLongitud() != null ? ruta.camionLongitud() : DEPOSITO_LNG;
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

                double lat = p.latitud() != null ? p.latitud() : -34.6083;
                double lng = p.longitud() != null ? p.longitud() : -58.4350;

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
                        String.format(Locale.US, "%.6f", lat),
                        String.format(Locale.US, "%.6f", lng));
                paradas.add(vista);

                if (confirmarParadaId != null && confirmarParadaId.equals(p.id()) && esPendiente) {
                    modelo.put("abrirModalConfirmar", true);
                    modelo.put("paradaSeleccionada", vista);
                }
                if (fallarParadaId != null && fallarParadaId.equals(p.id()) && esPendiente) {
                    modelo.put("abrirModalFallo", true);
                    modelo.put("paradaSeleccionada", vista);
                }
            }
        }

        modelo.put("paradas", paradas);
        modelo.put("pendientesTexto", pendientesCount == 1 ? "1 parada pendiente" : pendientesCount + " paradas pendientes");
        modelo.put("ordenResumen", paradas.stream()
                .map(p -> String.valueOf(p.orden()))
                .collect(Collectors.joining(" → ")));

        ctx.render(VISTA, modelo);
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

    private RutaDto obtenerRutaActivaSegura(Context ctx, Long camionId) {
        try {
            return logistica.obtenerRutaActivaCamion(ctx, camionId);
        } catch (Exception e) {
            return null;
        }
    }

    private List<RutaDto> listarHistorialSeguro(Context ctx, Long camionId) {
        try {
            return logistica.listarRutasCamion(ctx, camionId);
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
