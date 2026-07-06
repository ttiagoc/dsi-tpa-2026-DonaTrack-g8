package ar.edu.utn.frba.ddsi.common.models.entities;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Notificacion {
  private LocalDateTime fechaDeEnvio;
  private String mensaje;
  private MedioContacto contacto;
  private Boolean completada;

  public Notificacion(String mensaje, MedioContacto contacto) {
    this.fechaDeEnvio = null;
    this.mensaje = mensaje;
    this.contacto = contacto;
    this.completada = false;
  }
}
