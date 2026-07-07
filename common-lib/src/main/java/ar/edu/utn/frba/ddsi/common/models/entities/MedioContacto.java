package ar.edu.utn.frba.ddsi.common.models.entities;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedioContacto {
  private String valor;
  private TipoContacto tipoContacto;
}
