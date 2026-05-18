package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class Categoria {
  private String nombre;
  private Boolean pideEstado;
  private Boolean esPerecedero;
  private List<Subcategoria> subCategorias;

  public Boolean pideEstado() {
    return this.pideEstado;
  }
}
