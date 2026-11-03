package ar.edu.utn.frba.ddsi.donaciones.models.entities.gamificacion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.CascadeType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mision")
@Getter
@Setter
@NoArgsConstructor
public class Mision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String codigo;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 255)
  private String descripcion;

  @Column(nullable = false)
  private Integer meta;

  @ManyToOne(optional = false, cascade = CascadeType.ALL)
  @JoinColumn(name = "insignia_id", nullable = false)
  private Insignia insignia;

  public Mision(String codigo, String nombre, String descripcion, Integer meta, Insignia insignia) {
    this.codigo = codigo;
    this.nombre = nombre;
    this.descripcion = descripcion;
    this.meta = meta;
    this.insignia = insignia;
  }
}