package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class Ubicacion {
    private Double latitud;
    private Double longitud;
    private LocalDateTime timestamp;
    private Double velocidad;

    public Ubicacion(Double latitud, Double longitud, Double velocidad) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.timestamp = LocalDateTime.now();
        this.velocidad = velocidad;
    }
}
