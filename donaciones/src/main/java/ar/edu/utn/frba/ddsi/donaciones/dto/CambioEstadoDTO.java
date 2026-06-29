package ar.edu.utn.frba.ddsi.donaciones.dto;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoDTO {
    private Long donacionId;
    private TipoEstadoDonacion nuevoEstado;
    private String justificacion;
}
