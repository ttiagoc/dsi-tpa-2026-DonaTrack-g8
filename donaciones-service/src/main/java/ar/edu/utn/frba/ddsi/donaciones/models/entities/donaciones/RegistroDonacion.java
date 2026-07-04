package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegistroDonacion {
  private String descripcion;
  private LocalDateTime fecha;
  private List<Bien> bienes;

  public RegistroDonacion(String descripcion, List<Bien> bienes) {
    this.descripcion = descripcion;
    this.fecha = LocalDateTime.now();
    this.bienes = bienes;
  }
}