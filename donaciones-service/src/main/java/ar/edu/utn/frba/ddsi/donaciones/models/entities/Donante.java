package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class Donante {
  private List<RegistroDonacion> donaciones;

  public void agregarDonacion(RegistroDonacion donacion) {
    // TODO: Implementar
  }
}
