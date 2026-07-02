package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoInicioRutaDonante implements Evento {
    private final MedioContacto contacto;
    private final String urlMapa;

    @Override
    public String getMensaje() {
        return "Tu donacion ya está en camino. Seguí el recorrido del camión en tiempo real haciendo click en el siguiente mapa interactivo: "
                + this.urlMapa;
    }
}