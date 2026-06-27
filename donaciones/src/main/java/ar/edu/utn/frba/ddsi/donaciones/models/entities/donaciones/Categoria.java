package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.List;

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
  private List<Subcategoria> subCategorias;
}
