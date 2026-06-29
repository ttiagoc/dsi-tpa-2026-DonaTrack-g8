package ar.edu.utn.frba.ddsi.logistica.dto;

import java.util.List;
import lombok.Data;

@Data
public class ResultadoPlanificacionDTO {
    private List<CamionDTO> camiones;
    private List<Long> donacionesSinAsignar;
}