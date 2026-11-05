package ar.edu.utn.frba.ddsi.web.dto.donantes;

/** tipo: EMAIL, SMS o WHATSAPP (TipoContacto del backend). */
public record MedioContactoDto(
        String tipo,
        String valor
) {
    /** Para mostrar: "Email", "SMS" o "WhatsApp". */
    public String tipoEtiqueta() {
        return DonanteDetalle.etiquetaContacto(tipo);
    }
}
