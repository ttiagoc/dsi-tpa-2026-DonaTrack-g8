package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import lombok.Data;

@Data
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
