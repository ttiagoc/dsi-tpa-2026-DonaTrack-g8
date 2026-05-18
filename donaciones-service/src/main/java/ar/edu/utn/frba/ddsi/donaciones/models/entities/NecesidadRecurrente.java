package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class NecesidadRecurrente implements TipoNecesidad {
  private String periodo;

  @Override
  public Boolean estaSatisfecha(List<Donacion> donaciones) {
    // TODO: Implementar
    return false;
  }
}