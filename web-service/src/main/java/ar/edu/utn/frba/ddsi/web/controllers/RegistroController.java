package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.web.clients.AuthApiClient;
import ar.edu.utn.frba.ddsi.web.dto.registro.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RegistroEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RegistroPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RegistroPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RepresentanteRequest;
import ar.edu.utn.frba.ddsi.web.exceptions.ApiException;
import ar.edu.utn.frba.ddsi.web.sesion.Alertas;
import ar.edu.utn.frba.ddsi.web.sesion.SesionUsuario;
import io.javalin.http.Context;

/**
 * Registro de los tres tipos de usuario (req. 3). Aca solo se valida lo que no necesita al backend
 * (obligatorios, contrasenias iguales); las reglas de negocio las valida /api/auth/registro/* y su mensaje
 * se muestra arriba del formulario. Si sale bien, el backend ya devuelve la sesion: el usuario queda logueado.
 */
@Component
public class RegistroController {

    private static final String VISTA_PERSONA_HUMANA = "auth/registro-persona-humana.html.hbs";
    private static final String VISTA_PERSONA_JURIDICA = "auth/registro-persona-juridica.html.hbs";
    private static final String VISTA_ENTIDAD = "auth/registro-entidad.html.hbs";

    private final AuthApiClient auth;

    public RegistroController(AuthApiClient auth) {
        this.auth = auth;
    }

    public void selector(Context ctx) {
        ctx.render("auth/registro.html.hbs", Modelo.base(ctx));
    }

    public void formPersonaHumana(Context ctx) {
        mostrarFormulario(ctx, VISTA_PERSONA_HUMANA);
    }

    public void formPersonaJuridica(Context ctx) {
        mostrarFormulario(ctx, VISTA_PERSONA_JURIDICA);
    }

    public void formEntidad(Context ctx) {
        mostrarFormulario(ctx, VISTA_ENTIDAD);
    }

    public void registrarPersonaHumana(Context ctx) {
        Formulario form = new Formulario(ctx);
        String nombre = form.requerido("nombre", "El nombre");
        String apellido = form.requerido("apellido", "El apellido");
        LocalDate fechaNacimiento = form.fecha("fechaNacimiento", "La fecha de nacimiento");
        String dni = form.requerido("dni", "El DNI");
        String genero = form.requerido("genero", "El género");
        String direccion = form.requerido("direccion", "La dirección");
        Contactos contactos = leerContactos(form);
        String username = form.requerido("username", "El usuario");
        String password = leerPassword(form);

        registrar(ctx, form, VISTA_PERSONA_HUMANA, () -> auth.registrarPersonaHumana(
                new RegistroPersonaHumanaRequest(username, password, new PersonaHumanaRequest(
                        nombre, apellido, fechaNacimiento, dni, genero, direccion,
                        contactos.todos(), contactos.predeterminado()))));
    }

    public void registrarPersonaJuridica(Context ctx) {
        Formulario form = new Formulario(ctx);
        String razonSocial = form.requerido("razonSocial", "La razón social");
        String rubro = form.requerido("rubro", "El rubro");
        String tipo = form.requerido("tipo", "El tipo de organización");
        String cuit = form.requerido("cuit", "El CUIT");
        RepresentanteRequest representante = new RepresentanteRequest(
                form.requerido("representanteNombre", "El nombre del representante"),
                form.requerido("representanteApellido", "El apellido del representante"),
                form.requerido("representanteCorreo", "El correo del representante"));
        Contactos contactos = leerContactos(form);
        String username = form.requerido("username", "El usuario");
        String password = leerPassword(form);

        registrar(ctx, form, VISTA_PERSONA_JURIDICA, () -> auth.registrarPersonaJuridica(
                new RegistroPersonaJuridicaRequest(username, password, new PersonaJuridicaRequest(
                        razonSocial, rubro, tipo, cuit, List.of(representante),
                        contactos.todos(), contactos.predeterminado()))));
    }

    public void registrarEntidad(Context ctx) {
        Formulario form = new Formulario(ctx);
        String razonSocial = form.requerido("razonSocial", "La razón social");
        String direccion = form.requerido("direccion", "La dirección");
        String telefono = form.requerido("telefono", "El teléfono");
        String correo = form.requerido("correoRepresentante", "El correo del representante");
        String username = form.requerido("username", "El usuario");
        String password = leerPassword(form);

        registrar(ctx, form, VISTA_ENTIDAD, () -> auth.registrarEntidadBeneficiaria(
                new RegistroEntidadBeneficiariaRequest(username, password, new EntidadBeneficiariaRequest(
                        razonSocial, direccion, telefono, List.of(new MedioContactoRequest("EMAIL", correo))))));
    }

    // ---- Comun a los tres formularios ----

    private void registrar(Context ctx, Formulario form, String vista, Supplier<SesionUsuario> llamada) {
        if (form.tieneErrores()) {
            mostrarError(ctx, form, vista, "Revisá los campos marcados.");
            return;
        }
        try {
            SesionUsuario sesion = llamada.get();
            SesionUsuario.iniciar(ctx, sesion);
            Alertas.exito(ctx, "¡Te damos la bienvenida a DonaTrack, " + sesion.getUsuario().username() + "!");
            ctx.redirect(sesion.getUsuario().rol().panel());
        } catch (ApiException e) {
            if (!e.esErrorDelUsuario()) {
                throw e;
            }
            mostrarError(ctx, form, vista, e.getMessage());
        }
    }

    private String leerPassword(Formulario form) {
        String password = form.requerido("password", "La contraseña");
        String repetida = form.requerido("password2", "Repetir la contraseña");
        if (password != null && repetida != null && !password.equals(repetida)) {
            form.error("password2", "Las contraseñas no coinciden");
        }
        return password;
    }

    private record Contactos(List<MedioContactoRequest> todos, MedioContactoRequest predeterminado) {
    }

    /** Email obligatorio, telefono opcional (WhatsApp o SMS) y cual de los dos es el predeterminado. */
    private Contactos leerContactos(Formulario form) {
        List<MedioContactoRequest> contactos = new ArrayList<>();
        MedioContactoRequest email = null;
        MedioContactoRequest telefono = null;

        String correo = form.requerido("email", "El email");
        if (correo != null) {
            email = new MedioContactoRequest("EMAIL", correo);
            contactos.add(email);
        }

        String numero = form.valor("telefono");
        if (numero != null) {
            String tipo = "SMS".equals(form.valor("tipoTelefono")) ? "SMS" : "WHATSAPP";
            telefono = new MedioContactoRequest(tipo, numero);
            contactos.add(telefono);
        }

        if ("TELEFONO".equals(form.valor("contactoPredeterminado"))) {
            if (telefono == null) {
                form.error("telefono", "Cargá un teléfono para usarlo como contacto predeterminado");
            }
            return new Contactos(contactos, telefono);
        }
        return new Contactos(contactos, email);
    }

    private void mostrarFormulario(Context ctx, String vista) {
        ctx.render(vista, new Formulario(ctx).enModelo(Modelo.base(ctx), null));
    }

    private void mostrarError(Context ctx, Formulario form, String vista, String error) {
        ctx.status(400).render(vista, form.enModelo(Modelo.base(ctx), error));
    }
}
