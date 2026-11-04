package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.DonacionesApiClient;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.BienAltaRequest;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionAltaRequest;
import ar.edu.utn.frba.ddsi.web.dto.donaciones.SubcategoriaAltaRequest;
import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteResponse;
import ar.edu.utn.frba.ddsi.web.dto.donantes.MedioContactoDto;
import ar.edu.utn.frba.ddsi.web.dto.donantes.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

@Component
public class RecepcionAdminController {

    private static final String RUTA = "/admin/recepcion";
    private static final String VISTA = "admin/recepcion.html.hbs";

    private final DonacionesApiClient donaciones;

    public RecepcionAdminController(DonacionesApiClient donaciones) {
        this.donaciones = donaciones;
    }

    public void mostrarFormulario(Context ctx) {
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);
        cargarListas(ctx, modelo);

        String donanteParam = ctx.queryParam("donanteId");
        if (donanteParam != null && !donanteParam.isBlank()) {
            @SuppressWarnings("unchecked")
            Map<String, String> formValues = (Map<String, String>) modelo.getOrDefault("form", Map.of());
            Map<String, String> updated = new java.util.LinkedHashMap<>(formValues);
            updated.put("donanteId", donanteParam);
            modelo.put("form", updated);
        }

        if (ctx.queryParam("nuevoDonante") != null) {
            modelo.put("abrirModalDonante", true);
        }

        ctx.render(VISTA, modelo);
    }

    public void registrarDonacion(Context ctx) {
        Formulario form = new Formulario(ctx);
        String donanteIdStr = form.requerido("donanteId", "El donante");
        String subcategoria = form.requerido("subcategoria", "La subcategoría");
        String bienDescripcion = form.requerido("bienDescripcion", "El detalle del artículo");
        String cantidadStr = form.requerido("cantidad", "La cantidad");

        Long donanteId = null;
        if (donanteIdStr != null) {
            try {
                donanteId = Long.parseLong(donanteIdStr);
            } catch (NumberFormatException e) {
                form.error("donanteId", "Seleccioná un donante válido");
            }
        }

        Long cantidad = null;
        if (cantidadStr != null) {
            try {
                cantidad = Long.parseLong(cantidadStr);
                if (cantidad <= 0) {
                    form.error("cantidad", "La cantidad debe ser mayor a 0");
                }
            } catch (NumberFormatException e) {
                form.error("cantidad", "La cantidad debe ser un número entero");
            }
        }

        Double pesoKg = parseDouble(form.valor("pesoKg"));
        Double volumenM3 = parseDouble(form.valor("volumenM3"));
        LocalDate fechaVencimiento = parseFecha(form.valor("fechaVencimiento"));
        String estadoBien = form.valor("estadoBien") != null ? form.valor("estadoBien") : "NUEVO";
        String descripcion = form.valor("descripcion");
        if (descripcion == null || descripcion.isBlank()) {
            descripcion = "Recepción presencial en centro de acopio";
        }

        if (form.tieneErrores()) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), "Revisá los datos ingresados.");
            cargarListas(ctx, modelo);
            ctx.render(VISTA, modelo);
            return;
        }

        try {
            BienAltaRequest bien = new BienAltaRequest(
                    bienDescripcion,
                    cantidad,
                    pesoKg != null ? pesoKg : 1.0,
                    volumenM3 != null ? volumenM3 : 0.001,
                    estadoBien,
                    fechaVencimiento,
                    new SubcategoriaAltaRequest(subcategoria)
            );
            DonacionAltaRequest request = new DonacionAltaRequest(descripcion, donanteId, List.of(bien));
            donaciones.registrarDonacionPresencial(ctx, request);

            Alertas.exito(ctx, "La donación fue recibida e ingresada al depósito con éxito.");
            ctx.redirect("/admin/deposito");
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), e.getMessage());
            cargarListas(ctx, modelo);
            ctx.render(VISTA, modelo);
        }
    }

    public void crearDonanteHumana(Context ctx) {
        Formulario form = new Formulario(ctx);
        String nombre = form.requerido("nombre", "El nombre");
        String apellido = form.requerido("apellido", "El apellido");
        String dni = form.requerido("dni", "El DNI");
        LocalDate fechaNacimiento = parseFecha(form.valor("fechaNacimiento"));
        String email = form.email("email", "Email", false);
        String telefono = form.telefono("telefono", "Teléfono", false);
        String direccion = form.valor("direccion");

        if (form.tieneErrores()) {
            ctx.status(400);
            Map<String, Object> modelo = Modelo.base(ctx);
            cargarListas(ctx, modelo);
            modelo.put("abrirModalDonante", true);
            modelo.put("erroresDonante", form.errores());
            modelo.put("formDonante", form.valores());
            modelo.put("errorDonante", "Revisá los datos del donante.");
            ctx.render(VISTA, modelo);
            return;
        }

        try {
            List<MedioContactoDto> contactos = new ArrayList<>();
            if (email != null && !email.isBlank()) {
                contactos.add(new MedioContactoDto("EMAIL", email));
            }
            if (telefono != null && !telefono.isBlank()) {
                contactos.add(new MedioContactoDto("WHATSAPP", telefono));
            }
            MedioContactoDto contactoPred = contactos.isEmpty()
                    ? new MedioContactoDto("EMAIL", "presencial@donatrack.org")
                    : contactos.get(0);

            PersonaHumanaRequest request = new PersonaHumanaRequest(
                    nombre,
                    apellido,
                    fechaNacimiento != null ? fechaNacimiento : LocalDate.of(1990, 1, 1),
                    dni,
                    "Otro",
                    direccion != null ? direccion : "Sin dirección",
                    contactos.isEmpty() ? List.of(contactoPred) : contactos,
                    contactoPred
            );

            DonanteResponse nuevo = donaciones.crearDonantePersonaHumana(ctx, request);
            Alertas.exito(ctx, "Donante " + nuevo.nombre() + " registrado con éxito.");
            ctx.redirect(RUTA + "?donanteId=" + nuevo.id());
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = Modelo.base(ctx);
            cargarListas(ctx, modelo);
            modelo.put("abrirModalDonante", true);
            modelo.put("erroresDonante", form.errores());
            modelo.put("formDonante", form.valores());
            modelo.put("errorDonante", e.getMessage());
            ctx.render(VISTA, modelo);
        }
    }

    private void cargarListas(Context ctx, Map<String, Object> modelo) {
        try {
            modelo.put("catalogo", donaciones.catalogo());
            List<DonanteResponse> listaDonantes = donaciones.listarDonantes(ctx);
            modelo.put("donantes", listaDonantes);
            modelo.put("totalDonantes", listaDonantes != null ? listaDonantes.size() : 0);
            int enDeposito = donaciones.listarDonaciones(ctx, "EN_DEPOSITO").size();
            modelo.put("resumen", Map.of("ingresosMes", enDeposito));
        } catch (Exception ignored) {
        }
    }

    private Double parseDouble(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDate.parse(valor);
        } catch (Exception e) {
            return null;
        }
    }
}
