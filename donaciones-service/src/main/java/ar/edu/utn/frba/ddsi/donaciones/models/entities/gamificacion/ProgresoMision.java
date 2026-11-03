package ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "progreso_mision")
@Getter
@Setter
@NoArgsConstructor
public class ProgresoMision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "donante_id", nullable = false)
  private Long donanteId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "mision_id", nullable = false)
  private Mision mision;

  @Column(name = "progreso_actual", nullable = false)
  private Integer progresoActual = 0;

  @Column(nullable = false)
  private Boolean cumplida = false;

  @Column(name = "fecha_cumplimiento")
  private LocalDateTime fechaCumplimiento;

  public ProgresoMision(Long donanteId, Mision mision) {
    this.donanteId = donanteId;
    this.mision = mision;
    this.progresoActual = 0;
    this.cumplida = false;
  }

  public boolean actualizarProgreso(int cantidad) {
    this.progresoActual = cantidad;
    if (!this.cumplida && this.progresoActual >= mision.getMeta()) {
      this.cumplida = true;
      this.fechaCumplimiento = LocalDateTime.now();
      return true; // Acaba de ser completada
    }
    return false;
  }
}