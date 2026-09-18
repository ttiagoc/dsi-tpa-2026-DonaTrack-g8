package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Representante {
  private String nombre;
  private String apellido;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "valor", column = @Column(name = "correo_valor")),
      @AttributeOverride(name = "tipoContacto", column = @Column(name = "correo_tipo_contacto", length = 20))
  })
  private MedioContacto correo;
}
