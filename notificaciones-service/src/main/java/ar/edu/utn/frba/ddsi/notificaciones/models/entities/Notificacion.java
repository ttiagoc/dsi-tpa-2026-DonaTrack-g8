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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
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

  @Column(name = "tipo_evento", length = 50)
  private String tipoEvento;

  @Column(name = "destinatario_id")
  private Long destinatarioId;

  @Column(name = "donacion_id")
  private Long donacionId;

  public Notificacion(String mensaje, MedioContacto contacto) {
    this(mensaje, contacto, null, null, null);
  }

  public Notificacion(String mensaje, MedioContacto contacto, String tipoEvento, Long destinatarioId, Long donacionId) {
    this.id = null;
    this.fechaDeEnvio = null;
    this.mensaje = mensaje;
    this.contacto = contacto;
    this.completada = false;
    this.tipoEvento = tipoEvento;
    this.destinatarioId = destinatarioId;
    this.donacionId = donacionId;
  }
}

