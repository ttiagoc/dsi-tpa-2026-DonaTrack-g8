package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chofer {
    private String nombre;
    private String apellido;
}
