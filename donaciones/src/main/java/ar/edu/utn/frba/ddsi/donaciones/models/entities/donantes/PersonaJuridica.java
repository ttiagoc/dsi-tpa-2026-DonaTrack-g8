package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.util.List;

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
}
