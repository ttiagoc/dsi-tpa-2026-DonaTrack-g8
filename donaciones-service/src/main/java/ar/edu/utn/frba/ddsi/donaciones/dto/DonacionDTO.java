package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DonacionDTO {
    private Long id;
    private Long donanteId;
    private Long entidadBeneficiariaAsignadaId;
    private String subcategoria;
    private String estadoBienes;
    private LocalDate fechaVencimiento;
    private LocalDateTime fecha;
    private String estadoActual;
}
