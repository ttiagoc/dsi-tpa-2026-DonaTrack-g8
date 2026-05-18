package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class CambioEstado {
  private LocalDateTime fecha;
  private TipoEstadoDonacion estado;
  private String justificacion;
}