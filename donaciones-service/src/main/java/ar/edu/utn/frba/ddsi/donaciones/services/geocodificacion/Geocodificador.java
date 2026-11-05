package ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion;

/** Traduce una direccion escrita por una persona a coordenadas, y de paso verifica que exista. */
public interface Geocodificador {

    /**
     * @throws DireccionInvalidaException si el servicio respondio y la direccion no existe o esta incompleta
     * @throws GeocodificadorNoDisponibleException si no se pudo consultar (sin red, servicio caido, etc.)
     */
    Coordenadas ubicar(String direccion);
}
