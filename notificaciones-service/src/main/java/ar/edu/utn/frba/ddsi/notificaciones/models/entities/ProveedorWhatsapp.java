package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

public interface ProveedorWhatsapp {
  void enviarWhatsapp(String destino, String mensaje);
}
