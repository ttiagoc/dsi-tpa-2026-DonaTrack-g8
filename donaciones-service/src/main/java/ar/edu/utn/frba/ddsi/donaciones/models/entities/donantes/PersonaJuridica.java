package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaJuridica extends Donante {
  private String razonSocial;
  private String rubro;
  private String tipo;
  private String cuit;
  private List<Representante> representantes;

  public PersonaJuridica(Long id, List<MedioContacto> contactos, MedioContacto contactoPredeterminado,
      String razonSocial, String rubro, String tipo, String cuit, List<Representante> representantes) {
    super(id, contactos, contactoPredeterminado);
    this.razonSocial = razonSocial;
    this.rubro = rubro;
    this.tipo = tipo;
    this.cuit = cuit;
    this.representantes = representantes;
  }
}
