package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambioEstado {
  private LocalDateTime fecha;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TipoEstadoDonacion estado;

  @Column(length = 500)
  private String justificacion;
}
