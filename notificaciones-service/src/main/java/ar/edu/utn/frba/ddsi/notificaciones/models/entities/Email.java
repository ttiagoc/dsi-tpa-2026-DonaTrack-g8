package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;

@Component
public class Email implements EstrategiaNotificacion {

  private final ProveedorEmail proveedorEmail;
  private final TipoContacto tipoContacto = TipoContacto.EMAIL;

  public Email(ProveedorEmail proveedorEmail) {
    this.proveedorEmail = proveedorEmail;
  }

  @Override
  public TipoContacto getTipoContacto() {
    return this.tipoContacto;
  }

  @Override
  public void notificar(String valor, String mensaje) {
    proveedorEmail.enviarEmail(valor, mensaje);
  }
}
