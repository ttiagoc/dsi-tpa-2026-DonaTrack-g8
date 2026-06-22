package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.List;

import lombok.Data;

@Data
public class Categoria {
  private String nombre;
  private Boolean pideEstado;
  private Boolean esPerecedero;
  private List<Subcategoria> subCategorias;
}
