package ar.edu.utn.frba.ddsi.common.notificaciones;

import ar.edu.utn.frba.ddsi.common.MedioContacto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
public class Notificacion {
  private LocalDateTime fecha;
  private String mensaje;
  private MedioContacto contacto;
  private Boolean completada;
}
