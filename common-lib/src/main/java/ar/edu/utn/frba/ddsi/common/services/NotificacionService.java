package ar.edu.utn.frba.ddsi.common.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import ar.edu.utn.frba.ddsi.common.models.entities.*;

@Service
public class NotificacionService {
  private List<Notificacion> notificaciones = new ArrayList<>();

  public Notificacion enviarNotificacion(MedioContacto contacto, String mensaje) {
    if (contacto == null) {
      throw new IllegalArgumentException("El medio de contacto no puede ser nulo");
    }

    Notificacion notificacion = new Notificacion();
    notificacion.setContacto(contacto);
    notificacion.setMensaje(mensaje);
    notificacion.setFecha(LocalDateTime.now());
    notificacion.setCompletada(false);

    // Ejecuta la simulaciÃ³n llamando al metodo notificar de la clase concreta
    // (Email, WhatsApp, Telefono)
    contacto.notificar(mensaje);

    // Marca como completada
    notificacion.setCompletada(true);

    // Almacena en memoria para auditorÃ­a e historial
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
