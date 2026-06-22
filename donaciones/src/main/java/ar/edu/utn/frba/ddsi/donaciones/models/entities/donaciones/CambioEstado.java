package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import lombok.Data;

@Data
public class CambioEstado {
  private LocalDateTime fecha;
  private TipoEstadoDonacion estado;
  private String justificacion;

  public CambioEstado(LocalDateTime fecha, TipoEstadoDonacion estado, String justificacion) {
    this.fecha = fecha;
    this.estado = estado;
    this.justificacion = justificacion;
  }
}