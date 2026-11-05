package ar.edu.utn.frba.ddsi.web.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse;
import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteDetalle;
import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteResponse;
import ar.edu.utn.frba.ddsi.web.dto.donantes.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

/**
 * ABM de donantes del administrador (req. 15): listado con buscador y paginado, detalle, alta, edición y baja.
 * Los modales se abren por query param (?nuevo, ?ver=id, ?editar=id, ?eliminar=id), así funcionan sin JavaScript.
 * Los filtros (q, tipo, pagina) viajan en todos los links para volver a la misma página del listado.
 */
@Component
public class DonantesAdminController {

    private static final String VISTA = "admin/donantes.html.hbs";
    private static final String RUTA = "/admin/donantes";
    static final int POR_PAGINA = 20;

    private final DonacionesApiClient donaciones;

    public DonantesAdminController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void listar(Context ctx) {
        Map<String, Object> modelo = Modelo.base(ctx);
        Long ver = idParam(ctx, "ver");
        Long editar = idParam(ctx, "editar");
        Long eliminar = idParam(ctx, "eliminar");

        if (ctx.queryParam("nuevo") != null) {
            modelo.put("abrirModal", true);
            modelo.put("formDonante", Map.of("tipoPersona", "HUMANA"));
        } else if (ver != null) {
            modelo.put("detalle", donaciones.obtenerDetalleDonante(ctx, ver));
            modelo.put("abrirVer", true);
        } else if (editar != null) {
            DonanteDetalle donante = donaciones.obtenerDetalleDonante(ctx, editar);
            modelo.put("editado", donante);
            modelo.put("formDonante", FormularioDonante.valoresDe(donante));
            modelo.put("abrirModal", true);
        } else if (eliminar != null) {
            DonanteDetalle donante = donaciones.obtenerDetalleDonante(ctx, eliminar);
            modelo.put("aEliminar", donante);
            modelo.put(donante.cantidadDonaciones() > 0 ? "abrirNoSePuede" : "abrirEliminar", true);
        }
        mostrar(ctx, modelo);
    }

    public void crear(Context ctx) {
        Formulario form = new Formulario(ctx);
        FormularioDonante datos = FormularioDonante.alta(form);
        PersonaJuridicaRequest juridica = datos.esJuridica() ? datos.personaJuridica() : null;
        PersonaHumanaRequest humana = datos.esJuridica() ? null : datos.personaHumana();
        if (form.tieneErrores()) {
            volverAlModal(ctx, form, null, "Revisá los campos marcados.");
            return;
        }
        try {
            DonanteResponse nuevo = juridica != null
                    ? donaciones.crearDonantePersonaJuridica(ctx, juridica)
                    : donaciones.crearDonantePersonaHumana(ctx, humana);
            Alertas.exito(ctx, "Se registró el donante " + nuevo.nombre() + ".");
            ctx.redirect(RUTA + "?ver=" + nuevo.id());
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            volverAlModal(ctx, form, null, e.getMessage());
        }
    }

    public void actualizar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        DonanteDetalle actual = donaciones.obtenerDetalleDonante(ctx, id);
        Formulario form = new Formulario(ctx);
        FormularioDonante datos = FormularioDonante.edicion(form, actual);
        PersonaJuridicaRequest juridica = datos.esJuridica() ? datos.personaJuridica() : null;
        PersonaHumanaRequest humana = datos.esJuridica() ? null : datos.personaHumana();
        if (form.tieneErrores()) {
            volverAlModal(ctx, form, actual, "Revisá los campos marcados.");
            return;
        }
        try {
            DonanteResponse actualizado = juridica != null
                    ? donaciones.actualizarDonantePersonaJuridica(ctx, id, juridica)
                    : donaciones.actualizarDonantePersonaHumana(ctx, id, humana);
            Alertas.exito(ctx, "Se actualizaron los datos de " + actualizado.nombre() + ".");
            ctx.redirect(RUTA + consulta(ctx.formParam("volver")));
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            volverAlModal(ctx, form, actual, e.getMessage());
        }
    }

    public void eliminar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            donaciones.eliminarDonante(ctx, id);
            Alertas.exito(ctx, "Se eliminó el donante.");
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            Alertas.error(ctx, "No se pudo eliminar el donante: " + e.getMessage());
        }
        ctx.redirect(RUTA + consulta(ctx.formParam("volver")));
    }

    private void volverAlModal(Context ctx, Formulario form, DonanteDetalle editado, String mensaje) {
        Map<String, Object> modelo = Modelo.base(ctx);
        Map<String, String> valores = new LinkedHashMap<>(form.valores());
        if (editado != null) {
            // el tipo y el documento no se editan: se muestran como estaban
            valores.put("tipoPersona", editado.esJuridica() ? "JURIDICA" : "HUMANA");
            if (editado.documento() != null) {
                valores.put(editado.esJuridica() ? "cuit" : "dni", editado.documento());
            }
            modelo.put("editado", editado);
        }
        modelo.put("formDonante", valores);
        modelo.put("erroresDonante", form.errores());
        modelo.put("errorDonante", mensaje);
        modelo.put("abrirModal", true);
        ctx.status(400);
        mostrar(ctx, modelo);
    }

    private void mostrar(Context ctx, Map<String, Object> modelo) {
        List<DonanteResponse> todos = donaciones.listarDonantes(ctx);
        Map<Long, Resumen> resumenes = resumenPorDonante(donaciones.listarDonaciones(ctx, null));

        String q = texto(ctx.queryParam("q"));
        String tipo = "HUMANA".equals(ctx.queryParam("tipo")) || "JURIDICA".equals(ctx.queryParam("tipo"))
                ? ctx.queryParam("tipo") : null;
        String buscado = q != null ? normalizar(q) : null;

        List<Map<String, Object>> filas = new ArrayList<>();
        long humanas = 0;
        long conDonaciones = 0;
        for (DonanteResponse d : todos) {
            boolean esJuridica = d.tipo() != null && d.tipo().toLowerCase(Locale.ROOT).contains("jur");
            Resumen resumen = resumenes.getOrDefault(d.id(), Resumen.VACIO);
            if (!esJuridica) {
                humanas++;
            }
            if (resumen.cantidad > 0) {
                conDonaciones++;
            }
            if (tipo != null && esJuridica != "JURIDICA".equals(tipo)) {
                continue;
            }
            if (buscado != null && !coincide(d, buscado)) {
                continue;
            }
            Map<String, Object> fila = new HashMap<>();
            fila.put("id", d.id());
            fila.put("nombre", d.nombre());
            fila.put("documento", d.documento());
            fila.put("contacto", d.contactoPredeterminado());
            fila.put("esJuridica", esJuridica);
            fila.put("tipo", esJuridica ? "Persona jurídica" : "Persona humana");
            fila.put("donaciones", resumen.cantidad);
            fila.put("ultimaDonacion", resumen.ultima);
            filas.add(fila);
        }
        filas.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                Objects.toString(a.get("nombre"), ""), Objects.toString(b.get("nombre"), "")));

        int totalPaginas = Math.max(1, (filas.size() + POR_PAGINA - 1) / POR_PAGINA);
        int pagina = Math.min(Math.max(1, entero(ctx.queryParam("pagina"))), totalPaginas);
        int desde = (pagina - 1) * POR_PAGINA;
        List<Map<String, Object>> paginaActual = filas.subList(Math.min(desde, filas.size()),
                Math.min(desde + POR_PAGINA, filas.size()));

        // la consulta del listado (sin el modal) para que los links vuelvan a la misma página
        String filtros = armarConsulta(q, tipo, pagina > 1 ? pagina : null);
        modelo.put("donantes", paginaActual);
        modelo.put("filtros", Map.of("q", q != null ? q : "", "tipo", tipo != null ? tipo : ""));
        modelo.put("vacio", q != null || tipo != null
                ? Map.of("titulo", "Ningún donante coincide con la búsqueda.",
                        "bajada", "Probá con otro nombre, DNI, CUIT o email, o limpiá los filtros.")
                : Map.of("titulo", "Todavía no hay donantes registrados.",
                        "bajada", "Con el botón “Nuevo donante” cargás el primero. También se suman solos al crear "
                                + "su cuenta, o en bloque desde “Importar CSV”."));
        modelo.put("filtrosQuery", filtros);
        modelo.put("volverHref", RUTA + (filtros.isEmpty() ? "" : "?" + filtros));
        modelo.put("sufijo", filtros.isEmpty() ? "" : "&" + filtros);
        modelo.put("hayDonantes", !todos.isEmpty());
        modelo.put("resumen", Map.of(
                "total", todos.size(),
                "humanas", humanas,
                "juridicas", todos.size() - humanas,
                "conDonaciones", conDonaciones));

        Map<String, Object> paginacion = new HashMap<>();
        paginacion.put("pagina", pagina);
        paginacion.put("totalPaginas", totalPaginas);
        paginacion.put("desde", filas.isEmpty() ? 0 : desde + 1);
        paginacion.put("hasta", desde + paginaActual.size());
        paginacion.put("total", filas.size());
        paginacion.put("hayVarias", totalPaginas > 1);
        if (pagina > 1) {
            paginacion.put("anterior", RUTA + "?" + armarConsulta(q, tipo, pagina - 1));
        }
        if (pagina < totalPaginas) {
            paginacion.put("siguiente", RUTA + "?" + armarConsulta(q, tipo, pagina + 1));
        }
        modelo.put("paginacion", paginacion);

        ctx.render(VISTA, modelo);
    }

    private record Resumen(int cantidad, LocalDate ultima) {
        static final Resumen VACIO = new Resumen(0, null);
    }

    private static Map<Long, Resumen> resumenPorDonante(List<DonacionResponse> lista) {
        Map<Long, Resumen> resumenes = new HashMap<>();
        for (DonacionResponse d : lista) {
            if (d.donanteId() == null) {
                continue;
            }
            LocalDate fecha = d.fecha() != null ? d.fecha().toLocalDate() : null;
            resumenes.merge(d.donanteId(), new Resumen(1, fecha), (a, b) -> new Resumen(a.cantidad + b.cantidad,
                    a.ultima == null || (b.ultima != null && b.ultima.isAfter(a.ultima)) ? b.ultima : a.ultima));
        }
        return resumenes;
    }

    private static boolean coincide(DonanteResponse d, String buscado) {
        return normalizar(d.nombre()).contains(buscado)
                || normalizar(d.contactoPredeterminado()).contains(buscado)
                || soloDigitos(d.documento()).contains(soloDigitos(buscado)) && !soloDigitos(buscado).isEmpty();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String soloDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static String armarConsulta(String q, String tipo, Integer pagina) {
        List<String> partes = new ArrayList<>();
        if (q != null) {
            partes.add("q=" + URLEncoder.encode(q, StandardCharsets.UTF_8));
        }
        if (tipo != null) {
            partes.add("tipo=" + tipo);
        }
        if (pagina != null) {
            partes.add("pagina=" + pagina);
        }
        return String.join("&", partes);
    }

    /** Solo acepta la consulta que armó esta pantalla (q, tipo, pagina): nunca redirige a otro sitio. */
    private static String consulta(String volver) {
        if (volver == null || volver.isBlank() || !volver.matches("[A-Za-z0-9=&%+._\\-]*")) {
            return "";
        }
        return "?" + volver;
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static int entero(String valor) {
        try {
            return valor == null ? 1 : Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Long idParam(Context ctx, String nombre) {
        String valor = ctx.queryParam(nombre);
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
