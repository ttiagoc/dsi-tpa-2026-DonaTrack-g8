package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoAusenciaPlataforma implements Evento {
    private final MedioContacto contacto;

    @Override
    public String getMensaje() {
        return "¡Te extrañamos! Hace más de 20 días que no registrás actividad en DonaTrack. Tu ayuda hace la diferencia, sumate con una nueva donación.";
    }

}