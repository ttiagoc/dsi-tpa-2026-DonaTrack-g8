package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoEntregaFallida implements Evento {
    private final MedioContacto contacto;

    @Override
    public String getMensaje() {
        return "No se pudo realizar la entrega.";
    }
}
