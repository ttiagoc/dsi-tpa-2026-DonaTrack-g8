package ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "insignia")
@Getter
@Setter
@NoArgsConstructor
public class Insignia {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 255)
  private String descripcion;

  @Column(name = "icono_url", length = 500)
  private String iconoUrl;

  public Insignia(String nombre, String descripcion, String iconoUrl) {
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.iconoUrl = iconoUrl;
  }
}