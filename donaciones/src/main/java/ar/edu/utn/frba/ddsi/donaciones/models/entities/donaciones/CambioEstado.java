package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstado {
  private LocalDateTime fecha;
  private TipoEstadoDonacion estado;
  private String justificacion;
}