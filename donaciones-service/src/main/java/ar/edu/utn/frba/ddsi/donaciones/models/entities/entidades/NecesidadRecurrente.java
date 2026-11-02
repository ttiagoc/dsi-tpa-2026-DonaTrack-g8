package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.UnidadDeMedida;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NecesidadRecurrente implements TipoNecesidad {
  private Periodo periodo;

  @Override
  public Double cantidadAsignada(List<Donacion> donaciones, UnidadDeMedida unidad) {
    if (donaciones == null)
      return 0.0;

    return donaciones.stream()
        .filter(donacion -> donacion.estaDentroDelPeriodoActual(this.periodo))
        .mapToDouble(donacion -> donacion.cantidadBienesRecibidos(unidad))
        .sum();
  }
}
