package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class RegistroDonacion {
  private String descripcion;
  private LocalDate fecha;
  private List<Bien> bienes;

  public void segmentarDonacion(List<Bien> bienes) {
    // TODO: Implementar
  }
}
