package ar.edu.utn.frba.ddsi.web.server.templates;

/**
 * Variantes estandarizadas de botones en DonaTrack.
 * Cada tipo define su clase CSS base, tamaño y su ícono por defecto.
 */
public enum TipoBoton {
    ALTA("boton", "i-mas"),
    PAGINA("boton", null),
    VER("boton boton--secundario boton--chico", "i-ojo"),
    EDITAR("boton boton--secundario boton--chico", "i-lapiz"),
    ELIMINAR("boton boton--secundario boton--peligro boton--chico", "i-basura"),
    CONFIRMAR("boton", null),
    CONFIRMAR_PELIGRO("boton boton--peligro", null),
    CANCELAR("boton boton--secundario", null);

    private final String clase;
    private final String icono;

    TipoBoton(String clase, String icono) {
        this.clase = clase;
        this.icono = icono;
    }

    public String getClase() {
        return clase;
    }

    public String getIcono() {
        return icono;
    }

    public static TipoBoton desde(Object valor) {
        if (valor == null) {
            return PAGINA;
        }
        if (valor instanceof TipoBoton tipo) {
            return tipo;
        }
        String str = valor.toString().trim().toUpperCase().replace('-', '_');
        try {
            return TipoBoton.valueOf(str);
        } catch (IllegalArgumentException e) {
            return PAGINA;
        }
    }
}
