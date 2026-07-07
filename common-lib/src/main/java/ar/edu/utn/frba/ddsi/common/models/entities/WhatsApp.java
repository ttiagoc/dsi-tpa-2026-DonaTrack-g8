package ar.edu.utn.frba.ddsi.common.models.entities;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;

@Component
public class WhatsApp implements EstrategiaNotificacion {

  private final ProveedorWhatsapp proveedorWhatsapp;
  private final TipoContacto tipoContacto = TipoContacto.WHATSAPP;

  public WhatsApp(ProveedorWhatsapp proveedorWhatsapp) {
    this.proveedorWhatsapp = proveedorWhatsapp;
  }

  @Override
  public TipoContacto getTipoContacto() {
    return this.tipoContacto;
  }

  @Override
  public void notificar(String valor, String mensaje) {
    proveedorWhatsapp.enviarWhatsapp(valor, mensaje);
  }
}