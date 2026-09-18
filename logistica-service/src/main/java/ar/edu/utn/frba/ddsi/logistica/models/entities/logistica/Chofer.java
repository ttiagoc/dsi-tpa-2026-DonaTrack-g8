package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Chofer {
    private String nombre;
    private String apellido;
}
