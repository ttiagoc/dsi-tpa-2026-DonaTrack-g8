package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.MedioContacto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@Getter @Setter @NoArgsConstructor
public class Notificacion {
  private Date fecha;
  private String mensaje;
  private MedioContacto contacto;
  private Boolean completada;
}
