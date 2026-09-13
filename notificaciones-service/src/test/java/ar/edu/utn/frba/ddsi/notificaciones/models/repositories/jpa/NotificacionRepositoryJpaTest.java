package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.repositories.impl.JpaNotificacion;

@DisplayName("Persistencia de Notificacion")
class NotificacionRepositoryJpaTest extends PersistenciaTest {

  private final JpaNotificacion notificaciones = new JpaNotificacion();

  @Test
  @DisplayName("Persiste y recupera una notificacion con su medio de contacto")
  void persisteYRecuperaNotificacion() {
    MedioContacto contacto =
        new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

    Notificacion notificacion =
        new Notificacion("Mensaje de prueba", contacto);

    LocalDateTime fecha = LocalDateTime.now()
    notificacion.setFechaDeEnvio(fecha);

    Notificacion guardada = notificaciones.save(notificacion);

    assertTrue(guardada.getId() != null);

    nuevoRequest();

    Notificacion recuperada =
        notificaciones.findById(guardada.getId()).orElseThrow();

    assertEquals("Mensaje de prueba", recuperada.getMensaje());
    assertEquals(fecha, recuperada.getFechaDeEnvio());
    assertEquals("ana@mail.com", recuperada.getContacto().getValor());
    assertEquals(
        TipoContacto.EMAIL,
        recuperada.getContacto().getTipoContacto()
    );
    assertFalse(recuperada.getCompletada());
  }

  @Test
  @DisplayName("findAll recupera todas las notificaciones")
  void recuperaTodasLasNotificaciones() {
    MedioContacto email =
        new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

    MedioContacto telefono =
        new MedioContacto("1122334455", TipoContacto.SMS);

    notificaciones.save(
        new Notificacion("Primer mensaje", email)
    );

    notificaciones.save(
        new Notificacion("Segundo mensaje", telefono)
    );

    nuevoRequest();

    List<Notificacion> recuperadas = notificaciones.findAll();

    assertEquals(2, recuperadas.size());
  }

  @Test
  @DisplayName("Actualiza una notificacion existente")
  void actualizaNotificacion() {
    MedioContacto contacto =
        new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

    Notificacion notificacion =
        notificaciones.save(
            new Notificacion("Mensaje original", contacto)
        );

    Long id = notificacion.getId();

    notificacion.setMensaje("Mensaje actualizado");
    notificacion.setCompletada(true);

    notificaciones.save(notificacion);

    nuevoRequest();

    Notificacion recuperada =
        notificaciones.findById(id).orElseThrow();

    assertEquals("Mensaje actualizado", recuperada.getMensaje());
    assertTrue(recuperada.getCompletada());
  }

  @Test
  @DisplayName("Elimina una notificacion por id")
  void eliminaNotificacion() {
    MedioContacto contacto =
        new MedioContacto("ana@mail.com", TipoContacto.EMAIL);

    Notificacion notificacion =
        notificaciones.save(
            new Notificacion("Mensaje a eliminar", contacto)
        );

    Long id = notificacion.getId();

    assertTrue(notificaciones.deleteById(id));

    nuevoRequest();

    assertTrue(notificaciones.findById(id).isEmpty());
  }
}