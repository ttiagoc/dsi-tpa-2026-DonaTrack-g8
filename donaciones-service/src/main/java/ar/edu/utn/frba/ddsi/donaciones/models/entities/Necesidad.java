package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class Necesidad {
  private Subcategoria subcategoria;
  private TipoNecesidad tipoNecesidad;
  private String descripcion;
  private Double cantidad;
  private List<Donacion> donacionesAsignadas;
  private Boolean satisfecha;

  public void recibirDonacion(Donacion donacion) {
    // TODO: Implementar
  }
}