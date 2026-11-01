package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;

public interface TipoNecesidad {
  // cuantos bienes de las donaciones asignadas cuentan para la necesidad (las recurrentes, solo el periodo actual)
  Double cantidadAsignada(List<Donacion> donaciones);

  default Boolean estaSatisfecha(List<Donacion> donaciones, Long cantidadRequerida) {
    if (donaciones == null || donaciones.isEmpty())
      return false;
    return this.cantidadAsignada(donaciones) >= cantidadRequerida;
  }
}
