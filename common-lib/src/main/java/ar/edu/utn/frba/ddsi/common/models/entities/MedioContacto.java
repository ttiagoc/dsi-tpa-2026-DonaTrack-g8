package ar.edu.utn.frba.ddsi.common.models.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedioContacto {
  private String valor;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_contacto", length = 20)
  private TipoContacto tipoContacto;
}
