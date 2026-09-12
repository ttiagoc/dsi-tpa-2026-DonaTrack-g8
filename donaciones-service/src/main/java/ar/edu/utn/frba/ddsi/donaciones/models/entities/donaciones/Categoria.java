package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categoria")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Categoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String nombre;

  @Column(name = "pide_estado", nullable = false)
  private Boolean pideEstado;

  @Column(name = "es_perecedero", nullable = false)
  private Boolean esPerecedero;

  public Categoria(String nombre, Boolean pideEstado, Boolean esPerecedero) {
    this.nombre = nombre;
    this.pideEstado = pideEstado;
    this.esPerecedero = esPerecedero;
  }
}
