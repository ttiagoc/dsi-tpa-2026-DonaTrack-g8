package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient.ParadaDto;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient.RutaDto;
import ar.edu.utn.frba.ddsi.web.dto.auth.UsuarioSesion;
import ar.edu.utn.frba.ddsi.web.dto.comun.PaginaResponse;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

@Component
public class RutasAdminController {

    private static final String VISTA = "admin/rutas.html.hbs";
    private static final String RUTA = "/admin/rutas";

    private final LogisticaApiClient logistica;
    private final DonacionesApiClient donaciones;
    private final AuthApiClient auth;

    @Autowired
    public RutasAdminController(
            LogisticaApiClient logistica,
            DonacionesApiClient donaciones,
            @Autowired(required = false) AuthApiClient auth) {
        this.logistica = logistica;
        this.donaciones = donaciones;
        this.auth = auth;
    }

    public RutasAdminController(LogisticaApiClient logistica, DonacionesApiClient donaciones) {
        this(logistica, donaciones, null);
    }

    public void listar(Context ctx) {
        Map<String, Object> modelo = Modelo.base(ctx);
        String estadoFiltro = ctx.queryParam("estado");
        if (estadoFiltro == null || estadoFiltro.isBlank()) {
            estadoFiltro = "todas";
        }
        modelo.put("estadoFiltro", estadoFiltro);

        List<RutaDto> todas = new ArrayList<>(logistica.listarRutas(ctx));
        todas.sort(Comparator.comparing(RutaDto::id, Comparator.nullsLast(Comparator.reverseOrder())));

        Set<Long> enRutasActivas = new HashSet<>();
        for (RutaDto r : todas) {
            if (!r.esFinalizada()) {
                enRutasActivas.addAll(r.donacionIds());
            }
        }

        List<DonacionResponse> asignadas = listarAsignadasSeguro(ctx);
        long sinRuta = asignadas.stream()
                .filter(d -> d.id() != null && !enRutasActivas.contains(d.id()))
                .count();

        long planificadas = todas.stream().filter(RutaDto::esPlanificada).count();
        long enTraslado = todas.stream().filter(RutaDto::esEnTraslado).count();
        long finalizadas = todas.stream().filter(RutaDto::esFinalizada).count();

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("sinRuta", sinRuta);
        resumen.put("planificadas", planificadas);
        resumen.put("enTraslado", enTraslado);
        resumen.put("finalizadas", finalizadas);
        resumen.put("todas", todas.size());
        modelo.put("resumen", resumen);

        boolean esPlanificadas = "planificadas".equalsIgnoreCase(estadoFiltro);
        boolean esEnTraslado = "en_traslado".equalsIgnoreCase(estadoFiltro);
        boolean esFinalizadas = "finalizadas".equalsIgnoreCase(estadoFiltro);
        boolean esTodas = !esPlanificadas && !esEnTraslado && !esFinalizadas;

        modelo.put("esTabTodas", esTodas);
        modelo.put("esTabPlanificadas", esPlanificadas);
        modelo.put("esTabEnTraslado", esEnTraslado);
        modelo.put("esTabFinalizadas", esFinalizadas);

        Map<Long, String> choferesPorCamion = obtenerUsuariosChofer(ctx);

        List<RutaDto> filtradas = todas.stream()
                .filter(r -> {
                    if (esPlanificadas) return r.esPlanificada();
                    if (esEnTraslado) return r.esEnTraslado();
                    if (esFinalizadas) return r.esFinalizada();
                    return true;
                })
                .toList();

        List<RutaFila> filas = filtradas.stream()
                .map(r -> new RutaFila(
                        r.id(),
                        r.fechaFormateada(),
                        r.patenteCamion() != null ? r.patenteCamion() : "Sin camión",
                        r.choferNombreCompleto(),
                        r.camionId() != null ? choferesPorCamion.get(r.camionId()) : null,
                        r.paradasTexto(),
                        r.resumenEntregas(),
                        r.estado()))
                .toList();

        PaginaResponse<RutaFila> pagina = Paginador.paginarLista(ctx, filas, 10);
        modelo.put("rutas", pagina.elementos());
        modelo.put("paginacion", Paginador.armarModelo(ctx, pagina, RUTA));

        String verParam = ctx.queryParam("ver");
        if (verParam != null && !verParam.isBlank()) {
            try {
                Long verId = Long.parseLong(verParam);
                RutaDto seleccionada = todas.stream()
                        .filter(r -> verId.equals(r.id()))
                        .findFirst()
                        .orElseGet(() -> logistica.obtenerRuta(ctx, verId));
                if (seleccionada != null) {
                    modelo.put("abrirModalRuta", true);
                    modelo.put("rutaSeleccionada", armarDetalleModal(ctx, seleccionada, choferesPorCamion));
                }
            } catch (Exception e) {
                Alertas.error(ctx, "No se encontró la hoja de ruta solicitada.");
            }
        }

        ctx.render(VISTA, modelo);
    }

    public void planificar(Context ctx) {
        try {
            int antes = logistica.listarRutas(ctx).size();
            logistica.planificarRutas(ctx);
            int despues = logistica.listarRutas(ctx).size();
            int nuevas = Math.max(0, despues - antes);
            if (nuevas > 0) {
                Alertas.exito(ctx, nuevas == 1
                        ? "Se generó 1 nueva hoja de ruta según capacidad de la flota."
                        : "Se generaron " + nuevas + " nuevas hojas de ruta según capacidad de la flota.");
            } else {
                Alertas.info(ctx, "No hay donaciones asignadas pendientes de ruta o camiones disponibles para planificar.");
            }
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo ejecutar la planificación de rutas: " + e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    private RutaDetalleModal armarDetalleModal(Context ctx, RutaDto ruta, Map<Long, String> choferesPorCamion) {
        Map<Long, String> entidades = obtenerMapaEntidades(ctx);
        Map<Long, DonacionResponse> cacheDonaciones = new LinkedHashMap<>();
        List<ParadaDetalleModal> paradas = new ArrayList<>();
        boolean primeraPendienteMarcada = false;

        if (ruta.paradas() != null) {
            List<ParadaDto> ordenadas = new ArrayList<>(ruta.paradas());
            ordenadas.sort(Comparator.comparing(ParadaDto::orden, Comparator.nullsLast(Comparator.naturalOrder())));
            for (ParadaDto p : ordenadas) {
                String estado = p.estado() != null ? p.estado() : "PENDIENTE";
                boolean esPendiente = "PENDIENTE".equalsIgnoreCase(estado);
                boolean esEntregada = "ENTREGADA".equalsIgnoreCase(estado);
                boolean esFallida = "NO_RECIBIDA".equalsIgnoreCase(estado);
                boolean esActiva = esPendiente && !primeraPendienteMarcada;
                if (esActiva) {
                    primeraPendienteMarcada = true;
                }

                String entidadNombre = p.entidad() != null && entidades.containsKey(p.entidad())
                        ? entidades.get(p.entidad())
                        : (p.entidad() != null ? "Entidad #" + p.entidad() : "Destino asignado");

                List<String> bultos = new ArrayList<>();
                if (p.entregas() != null) {
                    for (Long donId : p.entregas()) {
                        if (donId == null) continue;
                        DonacionResponse d = cacheDonaciones.computeIfAbsent(donId, id -> obtenerDonacionSegura(ctx, id));
                        if (d != null && d.subcategoria() != null) {
                            bultos.add("Donación #" + donId + " · " + d.subcategoria() + " (" + d.totalUnidades() + " u)");
                        } else {
                            bultos.add("Donación #" + donId);
                        }
                    }
                }

                paradas.add(new ParadaDetalleModal(
                        p.id(),
                        p.orden() != null ? p.orden() : paradas.size() + 1,
                        entidadNombre,
                        p.destino() != null ? p.destino() : "Sin dirección informada",
                        estado,
                        esActiva,
                        esEntregada,
                        esFallida,
                        bultos));
            }
        }

        String choferCuenta = ruta.camionId() != null ? choferesPorCamion.get(ruta.camionId()) : null;
        return new RutaDetalleModal(
                ruta.id(),
                ruta.patenteCamion() != null ? ruta.patenteCamion() : "Sin camión",
                ruta.choferNombreCompleto(),
                choferCuenta,
                ruta.fechaFormateada(),
                ruta.estado(),
                paradas);
    }

    private List<DonacionResponse> listarAsignadasSeguro(Context ctx) {
        try {
            return donaciones.listarDonaciones(ctx, "ASIGNACION_REALIZADA");
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

    private Map<Long, String> obtenerUsuariosChofer(Context ctx) {
        if (auth == null) {
            return Map.of();
        }
        try {
            Map<Long, String> mapa = new LinkedHashMap<>();
            for (UsuarioSesion u : auth.listarUsuariosChofer(ctx)) {
                if (u != null && u.perfilId() != null && u.username() != null) {
                    mapa.put(u.perfilId(), u.username());
                }
            }
            return mapa;
        } catch (Exception e) {
            return Map.of();
        }
    }

    public record RutaFila(
            Long id,
            String fechaFormateada,
            String patenteCamion,
            String choferNombreCompleto,
            String choferCuenta,
            String paradasTexto,
            String resumenEntregas,
            String estado) {
    }

    public record RutaDetalleModal(
            Long id,
            String patenteCamion,
            String choferNombreCompleto,
            String choferCuenta,
            String fechaFormateada,
            String estado,
            List<ParadaDetalleModal> paradas) {
    }

    public record ParadaDetalleModal(
            Long id,
            Integer orden,
            String entidadNombre,
            String destino,
            String estado,
            boolean esActiva,
            boolean esEntregada,
            boolean esFallida,
            List<String> bultos) {
    }
}

