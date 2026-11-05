package ar.edu.utn.frba.ddsi.donaciones.config;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Coordenadas;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.DireccionInvalidaException;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Geocodificador;

/**
 * En los tests que levantan el servicio entero no se consulta Georef: no dependen de la red.
 * Toda direccion existe (en el Obelisco) salvo las que dicen "Inexistente".
 */
@Primary
@Component
public class GeocodificadorDePrueba implements Geocodificador {

    @Override
    public Coordenadas ubicar(String direccion) {
        if (direccion.contains("Inexistente")) {
            throw DireccionInvalidaException.inexistente(direccion);
        }
        return new Coordenadas(-34.6037, -58.3816);
    }
}
