package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.Telefono;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class EntidadBeneficiaria {
  private String razonSocial;
  private String direccion;
  private Telefono telefono;
  private List<Email> correoRepresentantes;
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(String razonSocial, String direccion, Telefono telefono, List<Email> correoRepresentantes) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.correoRepresentantes = correoRepresentantes;
    this.necesidades = new ArrayList<>();
  }

  public void registrarNecesidad(Necesidad necesidad) {
    this.necesidades.add(necesidad);
  }

  public void confirmarEntrega(Donacion donacion) {
    donacion.confirmarEntrega();
  }
}