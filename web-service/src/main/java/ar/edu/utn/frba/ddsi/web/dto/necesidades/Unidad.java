package ar.edu.utn.frba.ddsi.web.dto.necesidades;

import java.util.Arrays;
import java.util.Optional;

/**
 * En que se mide la cantidad de una necesidad. Mismos nombres que UnidadDeMedida de la API.
 * La donacion se cuenta en unidades y la API convierte con el peso o el volumen de cada bien.
 */
public enum Unidad {
    UNIDADES("unidades", "unidades"),
    KILOGRAMOS("kilogramos", "kg"),
    LITROS("litros", "litros");

    private final String etiqueta;
    private final String abreviatura;

    Unidad(String etiqueta, String abreviatura) {
        this.etiqueta = etiqueta;
        this.abreviatura = abreviatura;
    }

    /** Como se muestra en el select del formulario. */
    public String etiqueta() {
        return etiqueta;
    }

    /** Como se muestra al lado de un numero ("12 de 40 kg"). */
    public String abreviatura() {
        return abreviatura;
    }

    /** El valor que llega del select del formulario. */
    public static Optional<Unidad> deFormulario(String valor) {
        return Arrays.stream(values()).filter(u -> u.name().equals(valor)).findFirst();
    }

    /** La de una necesidad que devolvio la API (las viejas no traen unidad: son unidades). */
    public static Unidad de(NecesidadResponse necesidad) {
        return deFormulario(necesidad.unidad()).orElse(UNIDADES);
    }
}
