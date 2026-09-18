package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.time.LocalDateTime;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
