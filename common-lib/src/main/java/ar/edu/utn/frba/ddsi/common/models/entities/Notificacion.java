package ar.edu.utn.frba.ddsi.common.models.entities;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {
  private LocalDateTime fecha;
  private String mensaje;
  private MedioContacto contacto;
  private Boolean completada;
}
