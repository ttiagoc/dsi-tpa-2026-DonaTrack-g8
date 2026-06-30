package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.Email;
import ar.edu.utn.frba.ddsi.common.models.entities.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntidadBeneficiaria {
  private Long id;
  private String razonSocial;
  private String direccion;
  private Telefono telefono;
  private List<Email> correoRepresentantes;
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(String razonSocial, String direccion, Telefono telefono,
      List<Email> correoRepresentantes) {
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

  public void eliminarNecesidad(Long necesidadId) {
    this.necesidades.removeIf(n -> n.getId().equals(necesidadId));
  }
}