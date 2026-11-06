package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter 
@Setter
@NoArgsConstructor
public class Bien {
  @Column(length = 500)
  private String descripcion;

  @Column(length = 500)
  private String foto;

  private Long cantidad;

  @Column(name = "peso_kg_por_unidad")
  private Double pesoKgPorUnidad;

  @Column(name = "volumen_m3_por_unidad")
  private Double volumenM3PorUnidad;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private Subcategoria subcategoria;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_bien", length = 20)
  private EstadoBien estadoBien;

  @Column(name = "fecha_vencimiento")
  private LocalDate fechaVencimiento;

  public Bien(String descripcion, Long cantidad, Double pesoKgPorUnidad, Double volumenM3PorUnidad,
      Subcategoria subcategoria, EstadoBien estadoBien, LocalDate fechaVencimiento) {
    if (subcategoria != null) {
      if (subcategoria.esPerecedero() && fechaVencimiento == null) {
        throw new BusinessException("Un bien perecedero exige fecha de vencimiento");
      }
      if (subcategoria.pideEstado() && estadoBien == null) {
        throw new BusinessException("Un bien de esta categoría exige estado");
      }
    }
    this.descripcion = descripcion;
    this.foto = null;
    this.cantidad = cantidad;
    this.pesoKgPorUnidad = pesoKgPorUnidad;
    this.volumenM3PorUnidad = volumenM3PorUnidad;
    this.subcategoria = subcategoria;
    this.estadoBien = estadoBien;
    this.fechaVencimiento = fechaVencimiento;
  }

  public String generarKey() {
    String key = this.subcategoria.getNombre();

    if (this.subcategoria.esPerecedero() && this.fechaVencimiento != null) {
      key += "-" + this.fechaVencimiento;
    }

    if (this.subcategoria.pideEstado() && this.estadoBien != null) {
      key += "-" + this.estadoBien;
    }

    return key;
  }

  // los bienes nuevos siempre traen peso y volumen (la API los exige); los cargados antes
  // de esa validacion pueden no tenerlos: ahi aportan 0 en vez de romper el calculo
  public Double calcularPesoTotal() {
    if (this.pesoKgPorUnidad == null || this.cantidad == null)
      return 0.0;
    return this.pesoKgPorUnidad * this.cantidad;
  }

  public Double calcularVolumenTotal() {
    if (this.volumenM3PorUnidad == null || this.cantidad == null)
      return 0.0;
    return this.volumenM3PorUnidad * this.cantidad;
  }
}
