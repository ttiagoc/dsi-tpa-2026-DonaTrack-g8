package ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion;

/**
 * Un punto del mapa (WGS84, como lo usan OpenStreetMap y Leaflet) y como llama Georef a esa direccion
 * (por ejemplo "AV RIVADAVIA 6500, Comuna 7, Ciudad Autónoma de Buenos Aires"), para que la persona
 * verifique que encontramos la calle que quiso escribir.
 */
public record Coordenadas(double latitud, double longitud, String normalizada) {

    public Coordenadas(double latitud, double longitud) {
        this(latitud, longitud, null);
    }
}
