package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "registro_donacion")
@Data
@NoArgsConstructor
public class RegistroDonacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Donante tambien referencia a sus registros: se excluye para evitar recursion infinita
  @ManyToOne(optional = false)
  @JoinColumn(name = "donante_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Donante donante;

  @Column(length = 500)
  private String descripcion;

  private LocalDateTime fecha;

  // Solo sirve de entrada al segmentador: los bienes se persisten en cada Donacion
  @Transient
  private List<Bien> bienes;

  public RegistroDonacion(Donante donante, String descripcion, List<Bien> bienes) {
    this.donante = donante;
    this.descripcion = descripcion;
    this.fecha = LocalDateTime.now();
    this.bienes = bienes;
  }
}
