package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class NecesidadRecurrente implements TipoNecesidad {
  private Periodo periodo;

  @Override
  public Boolean estaSatisfecha(List<Donacion> donaciones, Double cantidadRequerida) {
    if (donaciones == null || donaciones.isEmpty()) return false;

    Double totalEnElPeriodo = 0.0;

    for (Donacion donacion : donaciones) {
      // Evaluamos si la donacion esta dentro del periodo actual
      if (donacion.estaDentroDelPeriodoActual(this.periodo)) {
        totalEnElPeriodo += donacion.cantidadBienesRecibidos();
      }
    }

    // Retorna true si llegamos o pasamos la meta en este periodo
    return totalEnElPeriodo >= cantidadRequerida;
  }
}