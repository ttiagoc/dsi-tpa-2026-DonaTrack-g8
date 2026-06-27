package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDate;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Bien {
  private String descripcion;
  private String foto;
  private Long cantidad;
  private String unidadMedida;
  private Subcategoria subcategoria;
  private EstadoBien estadoBien;
  private LocalDate fechaVencimiento;

  public String generarKey() {
    String key = this.subcategoria.getNombre();

    if (this.subcategoria.esPerecedero() && this.fechaVencimiento != null) {
      key += "-" + this.fechaVencimiento;
    }

    if (this.subcategoria.pideEstado() && this.estadoBien != null) {
      key += "-" + this.estadoBien;
    }

    return key;
  }
}
