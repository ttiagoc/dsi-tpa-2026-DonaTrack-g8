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
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
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

  public Double calcularPesoTotal() {
    return this.pesoKgPorUnidad * this.cantidad;
  }

  public Double calcularVolumenTotal() {
    return this.volumenM3PorUnidad * this.cantidad;
  }
}
