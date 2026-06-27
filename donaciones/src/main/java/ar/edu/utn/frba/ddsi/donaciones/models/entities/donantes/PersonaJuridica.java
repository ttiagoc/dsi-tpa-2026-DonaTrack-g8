package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoOrganizacion;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaJuridica extends Donante {
  private String razonSocial;
  private String rubro;
  private TipoOrganizacion tipo;
  private String cuit;
  private List<Representante> representantes;
}
