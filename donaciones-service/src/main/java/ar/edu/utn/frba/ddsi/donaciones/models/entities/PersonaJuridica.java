package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.MedioContacto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PersonaJuridica {
  private Donante donante;
  private String razonSocial;
  private String rubro;
  private TipoOrganizacion tipo;
  private String nroDocumento;
  private List<Representante> representantes;
  private List<MedioContacto> contactos;
  private MedioContacto contactoPredeterminado;
}
