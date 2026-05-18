package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Necesidad {
  private Subcategoria subcategoria;
  private TipoNecesidad tipoNecesidad;
  private String descripcion;
  private Double cantidad;
  private List<Donacion> donacionesAsignadas;
  private Boolean satisfecha;

  public Necesidad(Subcategoria subcategoria, TipoNecesidad tipoNecesidad, String descripcion, Double cantidad) {
    this.subcategoria = subcategoria;
    this.tipoNecesidad = tipoNecesidad;
    this.descripcion = descripcion;
    this.cantidad = cantidad;
    this.donacionesAsignadas = new ArrayList<>();
    this.satisfecha = false;
  }

  public void recibirDonacion(Donacion donacion) {
    this.donacionesAsignadas.add(donacion);
  }
}