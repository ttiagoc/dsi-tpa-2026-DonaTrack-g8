package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoDonacionAsignadaEntidad implements Evento {
    private final MedioContacto contacto;

    @Override
    public String getMensaje() {
        return "Una donación te ha sido asignada.";
    }
}
