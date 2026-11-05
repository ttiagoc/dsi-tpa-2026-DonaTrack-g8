package ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;

/** La direccion no existe o esta incompleta. El mensaje empieza con "La dirección" para que la web lo marque en ese campo. */
public class DireccionInvalidaException extends BusinessException {

    private static final String FORMATO = "Escribila como \"Calle y número, localidad, provincia\" "
            + "(por ejemplo: Av. Rivadavia 6500, Flores, CABA).";

    private DireccionInvalidaException(String mensaje) {
        super(mensaje);
    }

    public static DireccionInvalidaException inexistente(String direccion) {
        return new DireccionInvalidaException("La dirección \"" + direccion.trim() + "\" no existe o no la encontramos. "
                + "Revisá la calle y la altura: sin una dirección que exista no se puede registrar la entidad.");
    }

    public static DireccionInvalidaException incompleta() {
        return new DireccionInvalidaException("La dirección tiene que tener la altura, la localidad y la provincia. " + FORMATO);
    }
}
