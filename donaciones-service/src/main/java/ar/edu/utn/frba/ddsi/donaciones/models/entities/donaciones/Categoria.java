package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Categoria {
  private Long id;
  private String nombre;
  private Boolean pideEstado;
  private Boolean esPerecedero;

  public Categoria(String nombre, Boolean pideEstado, Boolean esPerecedero) {
    this.nombre = nombre;
    this.pideEstado = pideEstado;
    this.esPerecedero = esPerecedero;
  }
}
