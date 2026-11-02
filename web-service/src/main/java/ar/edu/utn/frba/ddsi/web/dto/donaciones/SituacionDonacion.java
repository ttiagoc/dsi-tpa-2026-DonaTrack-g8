package ar.edu.utn.frba.ddsi.web.dto.donaciones;

/**
 * El estado de una donacion asignada en palabras de la entidad, con la clase de su etiqueta.
 * Junta estados de logistica que para la entidad son lo mismo y separa "entregada" segun si ya confirmo
 * la recepcion (cargo fotos) o no.
 */
public enum SituacionDonacion {
    ASIGNADA("Asignada", "etiqueta--neutra", true),
    EN_CAMINO("En camino", "etiqueta--info", true),
    ENTREGA_FALLIDA("Entrega fallida", "etiqueta--error", true),
    PARA_CONFIRMAR("Entregada", "etiqueta--pendiente", false),
    RECIBIDA("Recibida", "", false),
    VENCIDA("Vencida", "etiqueta--neutra", false);

    private final String etiqueta;
    private final String clase;
    private final boolean porLlegar;

    SituacionDonacion(String etiqueta, String clase, boolean porLlegar) {
        this.etiqueta = etiqueta;
        this.clase = clase;
        this.porLlegar = porLlegar;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String clase() {
        return clase;
    }

    public boolean porLlegar() {
        return porLlegar;
    }

    public static SituacionDonacion de(DonacionResponse donacion) {
        return switch (donacion.estadoActual()) {
            case "EN_TRASLADO" -> EN_CAMINO;
            case "ENTREGA_FALLIDA" -> ENTREGA_FALLIDA;
            case "ENTREGADA" -> donacion.fotosRecepcion() == null || donacion.fotosRecepcion().isEmpty()
                    ? PARA_CONFIRMAR
                    : RECIBIDA;
            case "VENCIDA" -> VENCIDA;
            default -> ASIGNADA; // ASIGNACION_REALIZADA y LISTA_PARA_ENTREGAR
        };
    }
}
