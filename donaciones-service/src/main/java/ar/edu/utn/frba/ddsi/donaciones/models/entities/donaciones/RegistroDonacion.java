package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registro_donacion")
@Getter
@Setter
@NoArgsConstructor
public class RegistroDonacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "donante_id", nullable = false)
  private Donante donante;

  @Column(length = 500)
  private String descripcion;

  private LocalDateTime fecha;

  @OneToMany(mappedBy = "registroDonacion")
  private List<Donacion> donaciones = new ArrayList<>();

  public RegistroDonacion(Donante donante, String descripcion) {
    this.donante = donante;
    this.descripcion = descripcion;
    this.fecha = LocalDateTime.now();
  }

  public void agregarDonacion(Donacion donacion) {
    this.donaciones.add(donacion);
  }

  public List<Bien> getBienes() {
    return this.donaciones.stream()
        .flatMap(donacion -> donacion.getBienes().stream())
        .toList();
  }
}
