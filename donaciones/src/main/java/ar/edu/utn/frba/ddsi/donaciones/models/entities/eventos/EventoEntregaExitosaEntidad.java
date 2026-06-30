package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.time.format.DateTimeFormatter;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventoEntregaExitosaEntidad implements Evento {
    private final MedioContacto contacto;
    private final ComprobanteEntrega comprobante;

    @Override
    public String getMensaje() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return "Las donaciones fueron entregadas con éxito. Comprobante de entrega: "
                + "[Fecha/Hora: " + this.comprobante.getFechaHora().format(formatter) + " HS] "
                + "[Camión Responsable - Patente: " + this.comprobante.getPatenteCamion() + "].";
    }
}
