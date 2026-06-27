package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CambioEstado {
  private LocalDateTime fecha;
  private TipoEstadoDonacion estado;
  private String justificacion;
}