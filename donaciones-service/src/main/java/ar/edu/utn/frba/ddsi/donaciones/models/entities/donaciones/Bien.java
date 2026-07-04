package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDate;

import ar.edu.utn.frba.ddsi.common.models.enums.EstadoBien;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Bien {
  private String descripcion;
  private String foto;
  private Long cantidad;
  private Double pesoKgPorUnidad;
  private Double volumenM3PorUnidad;
  private Subcategoria subcategoria;
  private EstadoBien estadoBien;
  private LocalDate fechaVencimiento;

  public Bien(String descripcion, Long cantidad, Double pesoKgPorUnidad, Double volumenM3PorUnidad,
      Subcategoria subcategoria, EstadoBien estadoBien, LocalDate fechaVencimiento) {
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
