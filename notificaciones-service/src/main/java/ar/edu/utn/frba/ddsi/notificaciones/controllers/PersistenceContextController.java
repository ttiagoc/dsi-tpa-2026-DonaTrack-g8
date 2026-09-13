package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import io.javalin.Javalin;

/**
 * jpa-extras mantiene un EntityManager por hilo. Como Javalin reutiliza hilos entre requests,
 * se libera al terminar cada request para no arrastrar entidades cacheadas de un request a otro.
 */
@Component
public class PersistenceContextController implements JavalinController, WithSimplePersistenceUnit {

  @Override
  public void registerRoutes(Javalin app) {
    app.after(ctx -> {
      try {
        WithSimplePersistenceUnit.dispose();
      } catch (IllegalStateException transaccionAbierta) {
        rollbackTransaction();
        WithSimplePersistenceUnit.dispose();
      }
    });
  }
}