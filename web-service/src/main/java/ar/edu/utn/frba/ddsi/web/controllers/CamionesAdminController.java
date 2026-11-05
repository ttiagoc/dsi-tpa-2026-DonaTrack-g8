package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.clients.LogisticaApiClient;
import ar.edu.utn.frba.ddsi.web.dto.auth.UsuarioSesion;
import ar.edu.utn.frba.ddsi.web.dto.camiones.CamionRequest;
import ar.edu.utn.frba.ddsi.web.dto.camiones.CamionResponse;
import ar.edu.utn.frba.ddsi.web.dto.camiones.ChoferDto;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import io.javalin.http.Context;

@Component
public class CamionesAdminController {

    private static final String VISTA = "admin/camiones.html.hbs";
    private static final String RUTA = "/admin/camiones";
    private static final int MIN_PASSWORD_CHOFER = 8;

    private final LogisticaApiClient logistica;
    private final AuthApiClient auth;

    @Autowired
    public CamionesAdminController(LogisticaApiClient logistica, @Autowired(required = false) AuthApiClient auth) {
        this.logistica = logistica;
        this.auth = auth;
    }

    public CamionesAdminController(LogisticaApiClient logistica) {
        this(logistica, null);
    }

    public void listar(Context ctx) {
        Map<String, Object> modelo = new Formulario(ctx).enModelo(Modelo.base(ctx), null);
        String nuevoParam = ctx.queryParam("nuevo");
        String editarParam = ctx.queryParam("editar");
        String eliminarParam = ctx.queryParam("eliminar");

        modelo.put("esNuevo", editarParam == null || editarParam.isBlank());
        Map<Long, String> choferesPorCamion = obtenerUsuariosChofer(ctx);

        if (nuevoParam != null) {
            modelo.put("abrirModal", true);
            modelo.put("esNuevo", true);
        } else if (editarParam != null && !editarParam.isBlank()) {
            try {
                Long id = Long.parseLong(editarParam);
                CamionResponse camion = logistica.obtenerCamion(ctx, id);
                modelo.put("abrirModal", true);
                modelo.put("esNuevo", false);
                modelo.put("camionId", id);

                Map<String, String> valoresForm = new LinkedHashMap<>();
                valoresForm.put("patente", camion.patente());
                valoresForm.put("capacidadCarga", camion.capacidadCarga() != null ? String.valueOf(camion.capacidadCarga()) : "");
                valoresForm.put("capacidadVolumen", camion.capacidadVolumen() != null ? String.valueOf(camion.capacidadVolumen()) : "");
                valoresForm.put("altura", camion.altura() != null ? String.valueOf(camion.altura()) : "");
                if (camion.chofer() != null) {
                    valoresForm.put("choferNombre", camion.chofer().nombre());
                    valoresForm.put("choferApellido", camion.chofer().apellido());
                }
                String cuentaExistente = choferesPorCamion.get(id);
                if (cuentaExistente != null && !cuentaExistente.isBlank()) {
                    valoresForm.put("choferUsername", cuentaExistente);
                    modelo.put("tieneCuentaChofer", true);
                }
                modelo.put("form", valoresForm);
            } catch (Exception e) {
                Alertas.error(ctx, "No se pudo cargar el camión para editar.");
            }
        } else if (eliminarParam != null && !eliminarParam.isBlank()) {
            try {
                Long id = Long.parseLong(eliminarParam);
                CamionResponse camion = logistica.obtenerCamion(ctx, id);
                modelo.put("abrirEliminar", true);
                modelo.put("camionAEliminar", camion);
            } catch (Exception e) {
                Alertas.error(ctx, "No se encontró el camión a eliminar.");
            }
        }

        mostrar(ctx, modelo, choferesPorCamion);
    }

    public void crear(Context ctx) {
        Formulario form = new Formulario(ctx);
        CamionRequest request = validarFormulario(form);
        String choferUsername = form.valor("choferUsername");
        String choferPassword = form.valor("choferPassword");
        validarCuentaChofer(form, choferUsername, choferPassword, false);

        if (form.tieneErrores()) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", "Revisá los datos ingresados.");
            modelo.put("abrirModal", true);
            modelo.put("esNuevo", true);
            mostrar(ctx, modelo, obtenerUsuariosChofer(ctx));
            return;
        }

        try {
            CamionResponse creado = logistica.crearCamion(ctx, request);
            if (auth != null && choferUsername != null && !choferUsername.isBlank() && creado != null && creado.id() != null) {
                auth.configurarUsuarioChofer(ctx, creado.id(), choferUsername, choferPassword);
            }
            Alertas.exito(ctx, "Se registró el camión " + request.patente() + " correctamente.");
            ctx.redirect(RUTA);
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", e.getMessage());
            modelo.put("abrirModal", true);
            modelo.put("esNuevo", true);
            mostrar(ctx, modelo, obtenerUsuariosChofer(ctx));
        }
    }

    public void actualizar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Formulario form = new Formulario(ctx);
        CamionRequest request = validarFormulario(form);
        Map<Long, String> choferesPorCamion = obtenerUsuariosChofer(ctx);
        boolean yaTieneCuenta = choferesPorCamion.containsKey(id);
        String choferUsername = form.valor("choferUsername");
        String choferPassword = form.valor("choferPassword");
        validarCuentaChofer(form, choferUsername, choferPassword, yaTieneCuenta);

        if (form.tieneErrores()) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", "Revisá los datos ingresados.");
            modelo.put("abrirModal", true);
            modelo.put("esNuevo", false);
            modelo.put("camionId", id);
            modelo.put("tieneCuentaChofer", yaTieneCuenta);
            mostrar(ctx, modelo, choferesPorCamion);
            return;
        }

        try {
            logistica.actualizarCamion(ctx, id, request);
            if (auth != null && choferUsername != null && !choferUsername.isBlank()) {
                auth.configurarUsuarioChofer(ctx, id, choferUsername, choferPassword);
            }
            Alertas.exito(ctx, "Se actualizó el camión " + request.patente() + " correctamente.");
            ctx.redirect(RUTA);
        } catch (ApiException e) {
            ctx.status(400);
            Map<String, Object> modelo = form.enModelo(Modelo.base(ctx), null);
            modelo.put("errorGeneral", e.getMessage());
            modelo.put("abrirModal", true);
            modelo.put("esNuevo", false);
            modelo.put("camionId", id);
            modelo.put("tieneCuentaChofer", yaTieneCuenta);
            mostrar(ctx, modelo, choferesPorCamion);
        }
    }

    public void eliminar(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        try {
            logistica.eliminarCamion(ctx, id);
            Alertas.exito(ctx, "Se eliminó el camión correctamente.");
        } catch (ApiException e) {
            Alertas.error(ctx, "No se pudo eliminar el camión: " + e.getMessage());
        }
        ctx.redirect(RUTA);
    }

    private void mostrar(Context ctx, Map<String, Object> modelo, Map<Long, String> choferesPorCamion) {
        List<CamionResponse> camionesRaw = logistica.listarCamiones(ctx);
        List<CamionFila> camiones = camionesRaw.stream()
                .map(c -> new CamionFila(
                        c.id(),
                        c.patente(),
                        c.choferNombreCompleto(),
                        choferesPorCamion.get(c.id()),
                        c.capacidadCarga(),
                        c.capacidadVolumen(),
                        c.altura()))
                .toList();
        modelo.put("camiones", camiones);

        long total = camionesRaw.size();
        double cargaTotal = camionesRaw.stream()
                .map(CamionResponse::capacidadCarga)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        double volumenTotal = camionesRaw.stream()
                .map(CamionResponse::capacidadVolumen)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("total", total);
        resumen.put("cargaTotal", String.format("%.0f", cargaTotal));
        resumen.put("volumenTotal", String.format("%.1f", volumenTotal));
        modelo.put("resumen", resumen);

        ctx.render(VISTA, modelo);
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

    private void validarCuentaChofer(Formulario form, String username, String password, boolean yaTieneCuenta) {
        boolean tieneUser = username != null && !username.isBlank();
        boolean tienePass = password != null && !password.isBlank();

        if (!tieneUser && tienePass) {
            form.error("choferUsername", "Indicá el usuario de acceso del chofer");
        }
        if (tieneUser && !yaTieneCuenta && !tienePass) {
            form.error("choferPassword", "Indicá una contraseña para crear la cuenta del chofer");
        }
        if (tienePass && password.length() < MIN_PASSWORD_CHOFER) {
            form.error("choferPassword", "Tiene que tener al menos 8 caracteres");
        }
    }

    private CamionRequest validarFormulario(Formulario form) {
        String patente = form.requerido("patente", "La patente");
        if (patente != null) {
            patente = patente.toUpperCase();
        }

        Double capacidadCarga = parsePositivo(form, "capacidadCarga", "La capacidad de carga");
        Double capacidadVolumen = parsePositivo(form, "capacidadVolumen", "El volumen");
        Double altura = parsePositivo(form, "altura", "La altura");

        String choferNombre = form.requerido("choferNombre", "El nombre del chofer");
        String choferApellido = form.requerido("choferApellido", "El apellido del chofer");

        ChoferDto chofer = new ChoferDto(choferNombre, choferApellido);
        return new CamionRequest(patente, capacidadVolumen, altura, capacidadCarga, chofer);
    }

    private Double parsePositivo(Formulario form, String campo, String etiqueta) {
        String str = form.requerido(campo, etiqueta);
        if (str == null) {
            return null;
        }
        try {
            double val = Double.parseDouble(str.replace(",", "."));
            if (val <= 0) {
                form.error(campo, etiqueta + " debe ser un número mayor a 0");
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            form.error(campo, etiqueta + " debe ser un número válido");
            return null;
        }
    }

    public record CamionFila(
            Long id,
            String patente,
            String choferNombreCompleto,
            String choferUsername,
            Double capacidadCarga,
            Double capacidadVolumen,
            Double altura) {
    }
}

