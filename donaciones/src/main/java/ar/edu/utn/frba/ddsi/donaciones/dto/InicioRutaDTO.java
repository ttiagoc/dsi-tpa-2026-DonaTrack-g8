package ar.edu.utn.frba.ddsi.donaciones.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InicioRutaDTO {
    private Long rutaId;
    private List<ParadaDTO> paradas;
}
