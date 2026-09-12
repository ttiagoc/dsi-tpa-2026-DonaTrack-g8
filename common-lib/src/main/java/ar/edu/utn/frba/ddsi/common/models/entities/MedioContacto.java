package ar.edu.utn.frba.ddsi.common.models.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedioContacto {
  private String valor;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_contacto", length = 20)
  private TipoContacto tipoContacto;
}
