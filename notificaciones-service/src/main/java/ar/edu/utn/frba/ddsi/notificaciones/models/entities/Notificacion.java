package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import java.time.LocalDateTime;

import javax.persistence.AttributeOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notificacion")
@Data
@NoArgsConstructor
public class Notificacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fecha_de_envio")
  private LocalDateTime fechaDeEnvio;

  @Column(length = 500)
  private String mensaje;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(
          name = "valor",
          column = @Column(name = "contacto_valor")
      ),
      @AttributeOverride(
          name = "tipoContacto",
          column = @Column(
              name = "contacto_tipo_contacto",
              length = 20
          )
      )
  })
  private MedioContacto contacto;


  private Boolean completada;

  public Notificacion(String mensaje, MedioContacto contacto) {
    this.id = null;
    this.fechaDeEnvio = null;
    this.mensaje = mensaje;
    this.contacto = contacto;
    this.completada = false;
  }
}

