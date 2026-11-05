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
import ar.edu.utn.frba.ddsi.web.dto.registro.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RepresentanteRequest;
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
        String descripcion = form.valor("descripcion");
        if (descripcion == null || descripcion.isBlank()) {
            descripcion = "Recepción presencial en centro de acopio";
        }

        Long donanteId = null;
        if (donanteIdStr != null) {
            try {
                donanteId = Long.parseLong(donanteIdStr);
            } catch (NumberFormatException e) {
                form.error("donanteId", "Seleccioná un donante válido");
            }
        }

        List<String> subcategorias = ctx.formParams("subcategoria");
        List<String> descripciones = ctx.formParams("bienDescripcion");
        List<String> cantidades = ctx.formParams("cantidad");
        List<String> vencimientos = ctx.formParams("fechaVencimiento");
        List<String> pesos = ctx.formParams("pesoKg");
        List<String> volumenes = ctx.formParams("volumenM3");
        List<String> estados = ctx.formParams("estadoBien");

        int totalBienes = Math.max(descripciones.size(), subcategorias.size());
        if (totalBienes == 0) {
            form.error("bienDescripcion", "Tenés que ingresar al menos un artículo");
        }

        List<BienAltaRequest> bienes = new ArrayList<>();
        List<Map<String, Object>> bienesParaVista = new ArrayList<>();

        for (int i = 0; i < totalBienes; i++) {
            String subcat = i < subcategorias.size() ? subcategorias.get(i) : "";
            String bienDesc = i < descripciones.size() ? descripciones.get(i) : "";
            String cantStr = i < cantidades.size() ? cantidades.get(i) : "";
            String vencStr = i < vencimientos.size() ? vencimientos.get(i) : "";
            String pesoStr = i < pesos.size() ? pesos.get(i) : "";
            String volStr = i < volumenes.size() ? volumenes.get(i) : "";
            String est = (i < estados.size() && estados.get(i) != null && !estados.get(i).isBlank()) ? estados.get(i) : "NUEVO";

            Map<String, Object> itemVista = new java.util.LinkedHashMap<>();
            itemVista.put("indice", i + 1);
            itemVista.put("subcategoria", subcat);
            itemVista.put("bienDescripcion", bienDesc);
            itemVista.put("cantidad", cantStr);
            itemVista.put("fechaVencimiento", vencStr);
            itemVista.put("pesoKg", pesoStr != null && !pesoStr.isBlank() ? pesoStr : "1.0");
            itemVista.put("volumenM3", volStr != null && !volStr.isBlank() ? volStr : "0.001");
            itemVista.put("estadoBien", est);
            bienesParaVista.add(itemVista);

            if (subcat == null || subcat.isBlank()) {
                form.error("subcategoria", "La subcategoría es obligatorio");
            }
            if (bienDesc == null || bienDesc.isBlank()) {
                form.error("bienDescripcion", "El detalle del artículo es obligatorio");
            }

            Long cant = null;
            if (cantStr == null || cantStr.isBlank()) {
                form.error("cantidad", "La cantidad es obligatoria (artículo #" + (i + 1) + ")");
            } else {
                try {
                    cant = Long.parseLong(cantStr);
                    if (cant <= 0) {
                        form.error("cantidad", "La cantidad debe ser mayor a 0 (artículo #" + (i + 1) + ")");
                    }
                } catch (NumberFormatException e) {
                    form.error("cantidad", "La cantidad debe ser un número entero (artículo #" + (i + 1) + ")");
                }
            }

            Double peso = parseDouble(pesoStr);
            Double vol = parseDouble(volStr);
            LocalDate venc = parseFecha(vencStr);

            if (bienDesc != null && !bienDesc.isBlank() && subcat != null && !subcat.isBlank()) {
                bienes.add(new BienAltaRequest(
                        bienDesc,
                        cant != null ? cant : 1L,
                        peso != null ? peso : 1.0,
                        vol != null ? vol : 0.001,
                        est,
                        venc,
                        new SubcategoriaAltaRequest(subcat)
                ));
            }
        }

        if (form.tieneErrores() || bienes.isEmpty()) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), "Revisá los datos ingresados.");
            if (!bienesParaVista.isEmpty()) {
                modelo.put("bienes", bienesParaVista);
            }
            cargarListas(ctx, modelo);
            ctx.render(VISTA, modelo);
            return;
        }

        try {
            DonacionAltaRequest request = new DonacionAltaRequest(descripcion, donanteId, bienes);
            List<ar.edu.utn.frba.ddsi.web.dto.donaciones.DonacionResponse> donacionesCreadas = donaciones.registrarDonacionPresencial(ctx, request);

            int creadas = (donacionesCreadas != null && !donacionesCreadas.isEmpty()) ? donacionesCreadas.size() : 1;
            Alertas.exito(ctx, "Se registraron " + bienes.size() + " artículo(s) y se segmentaron en " + creadas + " donación(es) en depósito con éxito.");
            ctx.redirect("/admin/deposito");
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), e.getMessage());
            if (!bienesParaVista.isEmpty()) {
                modelo.put("bienes", bienesParaVista);
            }
            cargarListas(ctx, modelo);
            ctx.render(VISTA, modelo);
        }
    }

    public void crearDonanteHumana(Context ctx) {
        crearDonante(ctx);
    }

    public void crearDonante(Context ctx) {
        Formulario form = new Formulario(ctx);
        String tipoPersona = ctx.formParam("tipoPersona");
        boolean esJuridica = "JURIDICA".equalsIgnoreCase(tipoPersona);

        String email = form.email("email", "El email", true);
        String telefono = form.telefono("telefono", "El teléfono", false);
        String tipoTelefono = "SMS".equalsIgnoreCase(form.valor("tipoTelefono")) ? "SMS" : "WHATSAPP";
        String contactoPredeterminado = "TELEFONO".equalsIgnoreCase(form.valor("contactoPredeterminado")) ? "TELEFONO" : "EMAIL";

        DonanteResponse nuevo;
        try {
            if (esJuridica) {
                String razonSocial = form.requerido("razonSocial", "La razón social");
                String cuit = form.requerido("cuit", "El CUIT");
                String rubro = form.requerido("rubro", "El rubro");
                String tipo = form.requerido("tipo", "El tipo de organización");
                String repNombre = form.requerido("representanteNombre", "El nombre del representante");
                String repApellido = form.requerido("representanteApellido", "El apellido del representante");
                String repCorreo = form.email("representanteCorreo", "El correo del representante", true);

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

                List<MedioContactoRequest> contactos = new ArrayList<>();
                MedioContactoRequest emailMedio = new MedioContactoRequest("EMAIL", email);
                contactos.add(emailMedio);
                MedioContactoRequest telMedio = null;
                if (telefono != null && !telefono.isBlank()) {
                    telMedio = new MedioContactoRequest(tipoTelefono, telefono);
                    contactos.add(telMedio);
                }
                MedioContactoRequest contactoPred = ("TELEFONO".equals(contactoPredeterminado) && telMedio != null)
                        ? telMedio
                        : emailMedio;

                RepresentanteRequest rep = new RepresentanteRequest(repNombre, repApellido, repCorreo);
                PersonaJuridicaRequest reqJuridica = new PersonaJuridicaRequest(
                        razonSocial,
                        rubro,
                        tipo,
                        cuit,
                        List.of(rep),
                        contactos,
                        contactoPred
                );
                nuevo = donaciones.crearDonantePersonaJuridica(ctx, reqJuridica);
            } else {
                String nombre = form.requerido("nombre", "El nombre");
                String apellido = form.requerido("apellido", "El apellido");
                String dni = form.requerido("dni", "El DNI");
                LocalDate fechaNacimiento = parseFecha(form.valor("fechaNacimiento"));
                String genero = form.valor("genero");
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

                List<MedioContactoDto> contactos = new ArrayList<>();
                MedioContactoDto emailMedio = new MedioContactoDto("EMAIL", email);
                contactos.add(emailMedio);
                MedioContactoDto telMedio = null;
                if (telefono != null && !telefono.isBlank()) {
                    telMedio = new MedioContactoDto(tipoTelefono, telefono);
                    contactos.add(telMedio);
                }
                MedioContactoDto contactoPred = ("TELEFONO".equals(contactoPredeterminado) && telMedio != null)
                        ? telMedio
                        : emailMedio;

                PersonaHumanaRequest reqHumana = new PersonaHumanaRequest(
                        nombre,
                        apellido,
                        fechaNacimiento != null ? fechaNacimiento : LocalDate.of(1990, 1, 1),
                        dni,
                        genero != null && !genero.isBlank() ? genero : "Otro",
                        direccion != null && !direccion.isBlank() ? direccion : "Sin dirección",
                        contactos,
                        contactoPred
                );
                nuevo = donaciones.crearDonantePersonaHumana(ctx, reqHumana);
            }

            Alertas.exito(ctx, "Donante " + nuevo.nombre() + " registrado con éxito.");
            ctx.redirect(RUTA + "?donanteId=" + nuevo.id());
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = Modelo.base(ctx);
            cargarListas(ctx, modelo);
            modelo.put("abrirModalDonante", true);
            modelo.put("erroresDonante", Map.of("general", e.getMessage()));
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
