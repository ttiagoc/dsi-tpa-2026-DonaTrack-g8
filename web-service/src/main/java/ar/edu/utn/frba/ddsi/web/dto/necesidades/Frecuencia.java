package ar.edu.utn.frba.ddsi.web.dto.necesidades;

import java.util.Arrays;
import java.util.Optional;

/**
 * "¿Cada cuánto la necesitan?": un solo campo en el formulario que la API recibe como tipo + periodo
 * (una sola vez = extraordinaria; el resto = recurrente con su periodo).
 */
public enum Frecuencia {
    UNA_VEZ("Una sola vez", "extraordinaria", null),
    DIARIA("Todos los días", "recurrente", "DIARIO"),
    SEMANAL("Todas las semanas", "recurrente", "SEMANAL"),
    MENSUAL("Todos los meses", "recurrente", "MENSUAL"),
    ANUAL("Todos los años", "recurrente", "ANUAL");

    private final String etiqueta;
    private final String tipoNecesidad;
    private final String periodo;

    Frecuencia(String etiqueta, String tipoNecesidad, String periodo) {
        this.etiqueta = etiqueta;
        this.tipoNecesidad = tipoNecesidad;
        this.periodo = periodo;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String tipoNecesidad() {
        return tipoNecesidad;
    }

    public String periodo() {
        return periodo;
    }

    /** El valor que llega del select del formulario. */
    public static Optional<Frecuencia> deFormulario(String valor) {
        return Arrays.stream(values()).filter(f -> f.name().equals(valor)).findFirst();
    }

    /** La que corresponde a una necesidad que devolvio la API. */
    public static Frecuencia de(NecesidadResponse necesidad) {
        if (necesidad.periodo() == null) {
            return UNA_VEZ;
        }
        return Arrays.stream(values())
                .filter(f -> necesidad.periodo().equals(f.periodo))
                .findFirst()
                .orElse(UNA_VEZ);
    }
}
