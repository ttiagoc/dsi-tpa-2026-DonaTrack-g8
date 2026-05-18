package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.Telefono;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class EntidadBeneficiaria {
  private String razonSocial;
  private String direccion;
  private Telefono telefono;
  private List<Email> correoRepresentantes;
  private List<Necesidad> necesidades;

  public void registrarNecesidad(Necesidad necesidad) {
    // TODO: Implementar
  }

  public void confirmarEntrega(Donacion donacion) {
    // TODO: Implementar
  }
}