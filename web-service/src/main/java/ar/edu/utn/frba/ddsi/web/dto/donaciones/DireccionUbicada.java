package ar.edu.utn.frba.ddsi.web.dto.donaciones;

/** Lo que devuelve GET /direcciones/ubicar: donde queda y como la llama Georef (para verificarla). */
public record DireccionUbicada(double latitud, double longitud, String normalizada) {
}
