package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.UnidadDeMedida;

public interface TipoNecesidad {
  // cuanto de las donaciones asignadas cuenta para la necesidad, medido en su unidad
  // (las recurrentes, solo el periodo actual)
  Double cantidadAsignada(List<Donacion> donaciones, UnidadDeMedida unidad);

  default Boolean estaSatisfecha(List<Donacion> donaciones, Long cantidadRequerida, UnidadDeMedida unidad) {
    if (donaciones == null || donaciones.isEmpty())
      return false;
    return UnidadDeMedida.redondear(this.cantidadAsignada(donaciones, unidad)) >= cantidadRequerida;
  }
}
