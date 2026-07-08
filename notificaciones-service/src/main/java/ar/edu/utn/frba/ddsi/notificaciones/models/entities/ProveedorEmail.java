package ar.edu.utn.frba.ddsi.notificaciones.models.entities;

public interface ProveedorEmail {
  void enviarEmail(String destino, String mensaje);
}
