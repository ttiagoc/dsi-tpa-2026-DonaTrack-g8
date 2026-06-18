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

  public Necesidad(Subcategoria subcategoria, TipoNecesidad tipoNecesidad, String descripcion, Double cantidad) {
    this.subcategoria = subcategoria;
    this.tipoNecesidad = tipoNecesidad;
    this.descripcion = descripcion;
    this.cantidad = cantidad;
    this.donacionesAsignadas = new ArrayList<>();
  }

  public Boolean getSatisfecha() {
    if (this.tipoNecesidad == null) {
      return false;
    }
    return this.tipoNecesidad.estaSatisfecha(this.donacionesAsignadas, this.cantidad);
  }

  public void recibirDonacion(Donacion donacion) {
    this.donacionesAsignadas.add(donacion);
  }
}