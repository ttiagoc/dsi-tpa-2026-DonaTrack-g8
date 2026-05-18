package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class Bien {
  private String descripcion;
  private String foto;
  private Double cantidad;
  private String unidadMedida;
  private Subcategoria subcategoria;
  private Boolean esUsado;
  private LocalDate fechaVencimiento;

  public String generarKey() {
    String key = this.subcategoria.getNombre();

    if (this.subcategoria.esPerecedero() && this.fechaVencimiento != null) {
      key += "-" + this.fechaVencimiento.toString();
    }

    if (this.subcategoria.pideEstado() && this.esUsado != null) {
      key += "-" + this.esUsado.toString();
    }

    return key;
  }
}
