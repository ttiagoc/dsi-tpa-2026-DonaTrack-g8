package ar.edu.utn.frba.ddsi.web.dto.donantes;

public record DonanteResponse(
        Long id,
        String tipo,
        String nombre,
        String documento,
        String contactoPredeterminado
) {
    public String etiquetaBuscador() {
        StringBuilder sb = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) sb.append(nombre);
        if (documento != null && !documento.isBlank()) sb.append(" · DNI/CUIT ").append(documento);
        if (tipo != null && !tipo.isBlank()) sb.append(" (").append(tipo).append(")");
        return sb.toString();
    }

    public String idStr() {
        return String.valueOf(id);
    }
}
