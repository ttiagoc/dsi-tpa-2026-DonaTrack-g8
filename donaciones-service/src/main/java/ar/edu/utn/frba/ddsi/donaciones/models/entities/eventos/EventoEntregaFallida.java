package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoEntregaFallida implements EventoDonaciones {
    private final Donacion donacion;
    private final String motivo;
}
