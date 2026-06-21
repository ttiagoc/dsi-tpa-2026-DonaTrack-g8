package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class PersonaHumana extends Donante {
  private String nombre;
  private String apellido;
  private LocalDate fechaNacimiento;
  private String dni;
  private String genero;
  private String direccion;
}
