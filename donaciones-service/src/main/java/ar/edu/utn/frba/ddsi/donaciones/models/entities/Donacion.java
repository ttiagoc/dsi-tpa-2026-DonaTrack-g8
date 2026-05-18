package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class Donacion {
  private Subcategoria subcategoria;
  private Boolean esUsado;
  private LocalDate fechaVencimiento;
  private List<Bien> bienes;
  private List<CambioEstado> historialEstados;

  public TipoEstadoDonacion estadoActual() {
    // TODO: Implementar
    return null;
  }
}
