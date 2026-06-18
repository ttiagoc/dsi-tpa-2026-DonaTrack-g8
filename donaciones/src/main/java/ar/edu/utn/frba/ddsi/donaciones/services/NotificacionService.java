package ar.edu.utn.frba.ddsi.donaciones.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Notificacion;

@Service
public class NotificacionService {
  private final List<Notificacion> notificaciones = new ArrayList<>();

  public Notificacion enviarNotificacion(MedioContacto contacto, String mensaje) {
    if (contacto == null) {
      throw new IllegalArgumentException("El medio de contacto no puede ser nulo");
    }

    Notificacion notificacion = new Notificacion();
    notificacion.setContacto(contacto);
    notificacion.setMensaje(mensaje);
    notificacion.setFecha(LocalDateTime.now());
    notificacion.setCompletada(false);

    // Ejecuta la simulación llamando al metodo notificar de la clase concreta
    // (Email, WhatsApp, Telefono)
    contacto.notificar(mensaje);

    // Marca como completada
    notificacion.setCompletada(true);

    // Almacena en memoria para auditoría e historial
    notificaciones.add(notificacion);

    return notificacion;
  }

  public List<Notificacion> obtenerHistorial() {
    return new ArrayList<>(notificaciones);
  }

  public void limpiarHistorial() {
    notificaciones.clear();
  }
}
