package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NecesidadExtraordinaria implements TipoNecesidad {

  @Override
  public Boolean estaSatisfecha(List<Donacion> donaciones, Long cantidadRequerida) {
    return donaciones.stream()
        .mapToDouble(Donacion::cantidadBienesRecibidos)
        .sum() >= cantidadRequerida;
  }
}
