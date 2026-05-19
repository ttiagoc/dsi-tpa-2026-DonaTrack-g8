package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class NecesidadExtraordinaria implements TipoNecesidad {
  @Override
  public Boolean estaSatisfecha(List<Donacion> donaciones, Double cantidadRequerida) {
    return donaciones.stream()
        .mapToDouble(Donacion::cantidadBienesRecibidos)
        .sum() >= cantidadRequerida;
  }
}
