package ar.edu.utn.frba.ddsi.web.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.edu.utn.frba.ddsi.web.dto.donantes.DonanteDetalle;
import ar.edu.utn.frba.ddsi.web.dto.donantes.MedioContactoDto;
import ar.edu.utn.frba.ddsi.web.dto.donantes.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.PersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.web.dto.registro.RepresentanteRequest;

/**
 * Lee el formulario de donante (partials/form-donante) y arma el request para la API.
 * Lo usan la recepción en depósito (alta rápida) y la pantalla de donantes (alta y edición).
 * <p>
 * En la edición se pasa el donante actual: el DNI/CUIT no se cambia (la API no lo permite) y los contactos y
 * representantes que no se ven en el formulario se conservan.
 */
final class FormularioDonante {

    private final Formulario form;
    private final DonanteDetalle actual;
    private final boolean juridica;

    private FormularioDonante(Formulario form, DonanteDetalle actual) {
        this.form = form;
        this.actual = actual;
        this.juridica = actual != null ? actual.esJuridica() : "JURIDICA".equalsIgnoreCase(form.valor("tipoPersona"));
    }

    static FormularioDonante alta(Formulario form) {
        return new FormularioDonante(form, null);
    }

    static FormularioDonante edicion(Formulario form, DonanteDetalle actual) {
        return new FormularioDonante(form, actual);
    }

    boolean esJuridica() {
        return juridica;
    }

    /** Valida los datos de la persona humana. Si hay errores quedan en el Formulario. */
    PersonaHumanaRequest personaHumana() {
        String nombre = form.requerido("nombre", "El nombre");
        String apellido = form.requerido("apellido", "El apellido");
        String dni = actual == null ? form.requerido("dni", "El DNI") : null;
        LocalDate fechaNacimiento = fechaOpcional("fechaNacimiento", "La fecha de nacimiento");
        String genero = form.valor("genero");
        String direccion = form.valor("direccion");
        Contactos contactos = contactos();
        if (form.tieneErrores()) {
            return null;
        }
        List<MedioContactoDto> lista = contactos.lista.stream()
                .map(c -> new MedioContactoDto(c.tipo(), c.valor())).toList();
        MedioContactoDto predeterminado = new MedioContactoDto(contactos.predeterminado.tipo(),
                contactos.predeterminado.valor());
        if (actual != null) {
            // en la edición, lo que queda vacío conserva el valor que tenía
            return new PersonaHumanaRequest(nombre, apellido, fechaNacimiento, null, genero, direccion, lista,
                    predeterminado);
        }
        // la API los pide para el alta; en el depósito no siempre se conocen
        return new PersonaHumanaRequest(nombre, apellido,
                fechaNacimiento != null ? fechaNacimiento : LocalDate.of(1990, 1, 1),
                dni,
                genero != null ? genero : "Otro",
                direccion != null ? direccion : "Sin dirección",
                lista, predeterminado);
    }

    /** Valida los datos de la organización. Si hay errores quedan en el Formulario. */
    PersonaJuridicaRequest personaJuridica() {
        String razonSocial = form.requerido("razonSocial", "La razón social");
        String cuit = actual == null ? form.requerido("cuit", "El CUIT") : null;
        String rubro = form.requerido("rubro", "El rubro");
        String tipo = form.requerido("tipo", "El tipo de organización");
        String repNombre = form.requerido("representanteNombre", "El nombre del representante");
        String repApellido = form.requerido("representanteApellido", "El apellido del representante");
        String repCorreo = form.email("representanteCorreo", "El correo del representante", true);
        Contactos contactos = contactos();
        if (form.tieneErrores()) {
            return null;
        }
        List<RepresentanteRequest> representantes = new ArrayList<>();
        representantes.add(new RepresentanteRequest(repNombre, repApellido, repCorreo));
        if (actual != null && actual.representantes() != null && actual.representantes().size() > 1) {
            representantes.addAll(actual.representantes().subList(1, actual.representantes().size()));
        }
        return new PersonaJuridicaRequest(razonSocial, rubro, tipo, cuit, representantes, contactos.lista,
                contactos.predeterminado);
    }

    /** Los valores con que se abre el modal de edición (los mismos nombres que los campos del formulario). */
    static Map<String, String> valoresDe(DonanteDetalle d) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("tipoPersona", d.esJuridica() ? "JURIDICA" : "HUMANA");
        if (d.esJuridica()) {
            poner(valores, "razonSocial", d.razonSocial());
            poner(valores, "cuit", d.cuit());
            poner(valores, "rubro", d.rubro());
            poner(valores, "tipo", d.tipoOrganizacion());
            if (d.representantes() != null && !d.representantes().isEmpty()) {
                RepresentanteRequest rep = d.representantes().get(0);
                poner(valores, "representanteNombre", rep.nombre());
                poner(valores, "representanteApellido", rep.apellido());
                poner(valores, "representanteCorreo", rep.correo());
            }
        } else {
            poner(valores, "nombre", d.nombre());
            poner(valores, "apellido", d.apellido());
            poner(valores, "dni", d.dni());
            poner(valores, "fechaNacimiento", d.fechaNacimiento() != null ? d.fechaNacimiento().toString() : null);
            poner(valores, "genero", d.genero());
            poner(valores, "direccion", d.direccion());
        }
        if (d.email() != null) {
            poner(valores, "email", d.email().valor());
        }
        if (d.telefono() != null) {
            poner(valores, "telefono", d.telefono().valor());
            poner(valores, "tipoTelefono", "SMS".equalsIgnoreCase(d.telefono().tipo()) ? "SMS" : "WHATSAPP");
        }
        valores.put("contactoPredeterminado", d.prefiereTelefono() ? "TELEFONO" : "EMAIL");
        return valores;
    }

    private static void poner(Map<String, String> valores, String campo, String valor) {
        if (valor != null) {
            valores.put(campo, valor);
        }
    }

    private record Contactos(List<MedioContactoRequest> lista, MedioContactoRequest predeterminado) {
    }

    private Contactos contactos() {
        String email = form.email("email", "El email", true);
        String telefono = form.telefono("telefono", "El teléfono", false);
        String tipoTelefono = "SMS".equalsIgnoreCase(form.valor("tipoTelefono")) ? "SMS" : "WHATSAPP";
        boolean prefiereTelefono = "TELEFONO".equalsIgnoreCase(form.valor("contactoPredeterminado"));

        List<MedioContactoRequest> lista = new ArrayList<>();
        MedioContactoRequest medioEmail = new MedioContactoRequest("EMAIL", email);
        lista.add(medioEmail);
        MedioContactoRequest medioTelefono = null;
        if (telefono != null) {
            medioTelefono = new MedioContactoRequest(tipoTelefono, telefono);
            lista.add(medioTelefono);
        }
        if (actual != null) {
            actual.contactosExtra().forEach(c -> lista.add(new MedioContactoRequest(c.tipo(), c.valor())));
        }
        MedioContactoRequest predeterminado = prefiereTelefono && medioTelefono != null ? medioTelefono : medioEmail;
        return new Contactos(lista, predeterminado);
    }

    private LocalDate fechaOpcional(String campo, String etiqueta) {
        if (form.valor(campo) == null) {
            return null;
        }
        return form.fecha(campo, etiqueta);
    }
}
