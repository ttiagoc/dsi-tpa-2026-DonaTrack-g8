package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;

@Getter
@Setter
@NoArgsConstructor
public class NecesidadExtraordinaria implements TipoNecesidad {
  @Override
  public Boolean estaSatisfecha(List<Donacion> donaciones, Long cantidadRequerida) {
    return donaciones.stream()
        .mapToDouble(Donacion::cantidadBienesRecibidos)
        .sum() >= cantidadRequerida;
  }
}
