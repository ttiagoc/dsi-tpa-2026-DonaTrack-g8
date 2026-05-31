package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.MedioContacto;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public abstract class Donante {
  private List<RegistroDonacion> donaciones;
  private List<MedioContacto> contactos;
  private MedioContacto contactoPredeterminado;

  public Donante() {
    this.donaciones = new ArrayList<>();
    this.contactos = new ArrayList<>();
  }

  public void agregarDonacion(RegistroDonacion donacion) {
    if (donacion.getDonacionesSegmentadas() != null) {
      donacion.getDonacionesSegmentadas().forEach(d -> d.setDonante(this));
    }
    this.donaciones.add(donacion);
  }
}
