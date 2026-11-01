package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NecesidadExtraordinaria implements TipoNecesidad {

  @Override
  public Double cantidadAsignada(List<Donacion> donaciones) {
    if (donaciones == null)
      return 0.0;

    return donaciones.stream()
        .mapToDouble(Donacion::cantidadBienesRecibidos)
        .sum();
  }
}
