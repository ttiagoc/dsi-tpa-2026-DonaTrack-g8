package ar.edu.utn.frba.ddsi.logistica.dto;

import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import lombok.Data;

@Data
public class ResultadoPlanificacionDTO {
    private List<Ruta> rutasAsignadas;
    private List<DonacionDTO> donacionesSinAsignar;
}
