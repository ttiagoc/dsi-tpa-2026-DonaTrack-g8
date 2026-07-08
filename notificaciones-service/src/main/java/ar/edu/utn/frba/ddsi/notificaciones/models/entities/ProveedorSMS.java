package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

public interface ProveedorSMS {
  void enviarSMS(String destino, String mensaje);
}
