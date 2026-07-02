package ar.edu.utn.frba.ddsi.logistica.models.entities;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Ubicacion {
    private Double latitud;
    private Double longitud;
    private LocalDateTime timestamp;
    private Double velocidad;
}
