package ar.edu.utn.frba.ddsi.logistica.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RutaDTO {
    private Long id;
    private LocalDate fecha;
    private String estado;
    private Long camionId;
    private String choferNombre;
}
