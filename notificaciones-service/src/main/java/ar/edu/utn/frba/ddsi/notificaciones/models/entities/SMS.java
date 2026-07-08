package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;

@Component
public class SMS implements EstrategiaNotificacion {

  private final ProveedorSMS proveedorSMS;
  private final TipoContacto tipoContacto = TipoContacto.SMS;

  public SMS(ProveedorSMS proveedorSMS) {
    this.proveedorSMS = proveedorSMS;
  }

  @Override
  public TipoContacto getTipoContacto() {
    return this.tipoContacto;
  }

  @Override
  public void notificar(String valor, String mensaje) {
    proveedorSMS.enviarSMS(valor, mensaje);
  }
}
