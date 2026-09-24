package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CambioEstado {
  private LocalDateTime fecha;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TipoEstadoDonacion estado;

  @Column(length = 500)
  private String justificacion;

  @Column(name = "patente_camion", length = 20)
  private String patenteCamion;

  public CambioEstado(LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion) {
    this.fecha = fecha;
    this.estado = estado;
    this.justificacion = justificacion;
  }

  public CambioEstado(LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion,
      String patenteCamion) {
    this(fecha, estado, justificacion);
    this.patenteCamion = patenteCamion;
  }
}
