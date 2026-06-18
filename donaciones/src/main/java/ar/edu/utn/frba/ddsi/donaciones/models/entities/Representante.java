package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Representante {
  private String nombre;
  private String apellido;
  private Email correo;
}
