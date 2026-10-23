package ar.edu.utn.frba.ddsi.web.dto.registro;

/** tipo: EMAIL, SMS o WHATSAPP (TipoContacto del backend). */
public record MedioContactoRequest(String tipo, String valor) {
}
