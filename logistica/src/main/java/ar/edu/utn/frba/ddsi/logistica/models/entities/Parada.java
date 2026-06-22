package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.EntidadBeneficiariaDTO;
import lombok.Data;

@Data
public class Parada {
    private Integer orden;
    private String destino;
    private EntidadBeneficiariaDTO entidad;
    private List<DonacionDTO> entregas;
}
