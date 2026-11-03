package ar.edu.utn.frba.ddsi.web.dto.camiones;

public record CamionResponse(
    Long id,
    String patente,
    Double capacidadVolumen,
    Double altura,
    Double capacidadCarga,
    ChoferDto chofer
) {
    public String choferNombreCompleto() {
        if (chofer == null) {
            return "-";
        }
        String nombre = chofer.nombre() != null ? chofer.nombre() : "";
        String apellido = chofer.apellido() != null ? chofer.apellido() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? "-" : completo;
    }
}

