package ar.edu.utn.frba.ddsi.logistica.models.entities.eventos;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Ruta;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoInicioRuta implements EventoLogistica {
    private final Ruta ruta;
    private final String mapaUrl;
}
