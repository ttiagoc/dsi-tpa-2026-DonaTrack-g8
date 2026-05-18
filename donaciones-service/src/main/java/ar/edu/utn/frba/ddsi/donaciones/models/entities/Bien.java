package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class Bien {
  private String descripcion;
  private String foto;
  private Double cantidad;
  private String unidadMedida;
}
