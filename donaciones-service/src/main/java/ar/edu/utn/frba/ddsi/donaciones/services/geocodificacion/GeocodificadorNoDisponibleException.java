package ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion;

/** No se pudo consultar el servicio de direcciones: no dice nada sobre si la direccion existe. */
public class GeocodificadorNoDisponibleException extends RuntimeException {

    public GeocodificadorNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
