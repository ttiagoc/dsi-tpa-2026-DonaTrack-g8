package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "necesidad")
@Data
@NoArgsConstructor
public class Necesidad {
  private static final String RECURRENTE = "RECURRENTE";
  private static final String EXTRAORDINARIA = "EXTRAORDINARIA";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "subcategoria_id", nullable = false)
  private Subcategoria subcategoria;

  // TipoNecesidad es un Strategy (interfaz), no una entidad: se aplana en tipo_necesidad + periodo
  @Transient
  private TipoNecesidad tipoNecesidad;

  @Column(name = "tipo_necesidad", length = 20)
  @Setter(AccessLevel.NONE)
  private String tipo;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  @Setter(AccessLevel.NONE)
  private Periodo periodo;

  @Column(length = 500)
  private String descripcion;

  private Long cantidad;

  @ManyToMany
  @JoinTable(name = "necesidad_donaciones_asignadas",
      joinColumns = @JoinColumn(name = "necesidad_id"),
      inverseJoinColumns = @JoinColumn(name = "donacion_id"))
  private List<Donacion> donacionesAsignadas;

  public Necesidad(Subcategoria subcategoria, TipoNecesidad tipoNecesidad, String descripcion, Long cantidad) {
    this.subcategoria = subcategoria;
    this.setTipoNecesidad(tipoNecesidad);
    this.descripcion = descripcion;
    this.cantidad = cantidad;
    this.donacionesAsignadas = new ArrayList<>();
  }

  public void setTipoNecesidad(TipoNecesidad tipoNecesidad) {
    this.tipoNecesidad = tipoNecesidad;
    if (tipoNecesidad instanceof NecesidadRecurrente recurrente) {
      this.tipo = RECURRENTE;
      this.periodo = recurrente.getPeriodo();
    } else if (tipoNecesidad instanceof NecesidadExtraordinaria) {
      this.tipo = EXTRAORDINARIA;
      this.periodo = null;
    } else {
      this.tipo = null;
      this.periodo = null;
    }
  }

  @PostLoad
  private void reconstruirTipoNecesidad() {
    if (RECURRENTE.equals(this.tipo)) {
      this.tipoNecesidad = new NecesidadRecurrente(this.periodo);
    } else if (EXTRAORDINARIA.equals(this.tipo)) {
      this.tipoNecesidad = new NecesidadExtraordinaria();
    }
  }

  public Boolean estaSatisfecha() {
    if (this.tipoNecesidad == null) {
      return false;
    }
    return this.tipoNecesidad.estaSatisfecha(this.donacionesAsignadas, this.cantidad);
  }

  public void asignarDonacion(Donacion donacion) {
    this.donacionesAsignadas.add(donacion);
  }
}
