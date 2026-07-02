package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Representante {
  private String nombre;
  private String apellido;
  private MedioContacto correo;
}
