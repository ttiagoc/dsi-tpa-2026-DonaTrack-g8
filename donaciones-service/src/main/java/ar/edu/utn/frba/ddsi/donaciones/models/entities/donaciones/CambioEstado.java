package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cambio_estado")
@Getter
@Setter
@NoArgsConstructor
public class CambioEstado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "donacion_id", nullable = false)
  private Donacion donacion;

  private LocalDateTime fecha;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TipoEstadoDonacion estado;

  @Column(length = 500)
  private String justificacion;

  @Column(name = "patente_camion", length = 20)
  private String patenteCamion;

  public CambioEstado(Donacion donacion, LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion) {
    this.donacion = donacion;
    this.fecha = fecha;
    this.estado = estado;
    this.justificacion = justificacion;
  }

  public CambioEstado(Donacion donacion, LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion,
      String patenteCamion) {
    this(donacion, fecha, estado, justificacion);
    this.patenteCamion = patenteCamion;
  }
}
