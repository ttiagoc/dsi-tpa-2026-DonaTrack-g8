package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import lombok.Data;

@Data
public class Necesidad {
  private Subcategoria subcategoria;
  private TipoNecesidad tipoNecesidad;
  private String descripcion;
  private Long cantidad;
  private List<Donacion> donacionesAsignadas;

  public Necesidad(Subcategoria subcategoria, TipoNecesidad tipoNecesidad, String descripcion, Long cantidad) {
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

  public void asignarDonacion(Donacion donacion) {
    this.donacionesAsignadas.add(donacion);
  }
}