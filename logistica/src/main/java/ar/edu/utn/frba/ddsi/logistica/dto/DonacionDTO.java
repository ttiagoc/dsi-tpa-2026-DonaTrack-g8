package ar.edu.utn.frba.ddsi.logistica.dto;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import lombok.Data;

@Data
public class DonacionDTO {
    private Long id;
    private TipoEstadoDonacion estado;
}
