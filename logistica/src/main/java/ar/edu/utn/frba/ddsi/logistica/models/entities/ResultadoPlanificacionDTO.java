package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.List;
import lombok.Data;

@Data
public class ResultadoPlanificacionDTO {
    private List<Ruta> rutasAsignadas;
    private List<DonacionDTO> donacionesSinAsignar;
}
