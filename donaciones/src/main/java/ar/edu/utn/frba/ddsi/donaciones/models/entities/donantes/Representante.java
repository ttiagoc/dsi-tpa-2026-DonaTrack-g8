package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import lombok.Data;

@Data
public class Representante {
  private String nombre;
  private String apellido;
  private Email correo;
}
