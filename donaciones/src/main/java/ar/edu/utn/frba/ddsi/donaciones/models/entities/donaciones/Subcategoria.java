package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Subcategoria {
  private String nombre;
  private Categoria categoria;

  public Boolean esPerecedero() {
    return this.categoria != null && this.categoria.getEsPerecedero();
  }

  public Boolean pideEstado() {
    return this.categoria != null && this.categoria.getPideEstado();
  }
}
