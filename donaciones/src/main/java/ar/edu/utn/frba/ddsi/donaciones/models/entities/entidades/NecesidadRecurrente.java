package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.Data;

@Data
public class NecesidadRecurrente implements TipoNecesidad {
  private Periodo periodo;

  @Override
  public Boolean estaSatisfecha(List<Donacion> donaciones, Long cantidadRequerida) {
    if (donaciones == null || donaciones.isEmpty())
      return false;

    Double totalEnElPeriodo = donaciones.stream()
        .filter(donacion -> donacion.estaDentroDelPeriodoActual(this.periodo))
        .mapToDouble(Donacion::cantidadBienesRecibidos)
        .sum();

    // Retorna true si llegamos o pasamos la meta en este periodo
    return totalEnElPeriodo >= cantidadRequerida;
  }
}