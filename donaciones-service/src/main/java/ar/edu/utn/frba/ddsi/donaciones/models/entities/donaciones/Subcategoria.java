package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subcategoria")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Subcategoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String nombre;

  @ManyToOne(optional = false)
  @JoinColumn(name = "categoria_id", nullable = false)
  private Categoria categoria;

  public Subcategoria(String nombre, Categoria categoria) {
    this.nombre = nombre;
    this.categoria = categoria;
  }

  public Boolean esPerecedero() {
    return this.categoria != null && this.categoria.getEsPerecedero();
  }

  public Boolean pideEstado() {
    return this.categoria != null && this.categoria.getPideEstado();
  }
}
