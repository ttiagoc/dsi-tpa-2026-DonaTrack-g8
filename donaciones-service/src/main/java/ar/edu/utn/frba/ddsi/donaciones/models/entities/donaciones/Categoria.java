package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Categoria {
  private String nombre;
  private Boolean pideEstado;
  private Boolean esPerecedero;
}
