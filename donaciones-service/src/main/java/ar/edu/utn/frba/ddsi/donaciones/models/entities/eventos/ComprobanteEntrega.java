package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteEntrega {
    String patenteCamion;
    LocalDateTime fechaHora;
}
