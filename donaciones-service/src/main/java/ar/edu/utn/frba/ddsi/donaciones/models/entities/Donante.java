package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Donante {
  private List<RegistroDonacion> donaciones;

  public Donante() {
    this.donaciones = new ArrayList<>();
  }

  public void agregarDonacion(RegistroDonacion donacion) {
    this.donaciones.add(donacion);
  }
}
