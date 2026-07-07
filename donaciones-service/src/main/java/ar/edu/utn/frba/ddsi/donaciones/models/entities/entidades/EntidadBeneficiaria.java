package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntidadBeneficiaria {
  private Long id;
  private String razonSocial;
  private String direccion;
  private MedioContacto telefono;
  private List<MedioContacto> correoRepresentantes;
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(String razonSocial, String direccion, String valorTelefono,
      List<MedioContacto> correoRepresentantes) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = new MedioContacto(valorTelefono, TipoContacto.SMS);
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