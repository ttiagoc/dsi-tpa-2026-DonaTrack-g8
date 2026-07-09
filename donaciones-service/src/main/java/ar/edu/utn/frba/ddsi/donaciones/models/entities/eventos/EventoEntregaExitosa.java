package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoEntregaExitosa implements EventoDonaciones {
    private final EntidadBeneficiaria entidad;
    private final List<Donacion> donaciones;
    private final ComprobanteEntrega comprobante;
}
