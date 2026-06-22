package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import lombok.Data;

@Data
public abstract class Donante {
  private Long id;
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
