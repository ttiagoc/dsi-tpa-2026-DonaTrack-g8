package ar.edu.utn.frba.ddsi.notificaciones.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Configura la conexion de la unidad de persistencia (persistence.xml) a partir de
 * application.properties: PostgreSQL para el despliegue local, HSQLDB en los tests.
 * El EntityManagerFactory se crea al levantar el servicio, asi Hibernate genera el esquema
 * sin esperar al primer request.
 */
@Configuration
public class PersistenceConfig {

  public PersistenceConfig(Environment env) {
    try {
      WithSimplePersistenceUnit.configure(properties -> properties
          .set("hibernate.connection.driver_class",
              env.getRequiredProperty("notificaciones.db.driver"))
          .set("hibernate.connection.url",
              env.getRequiredProperty("notificaciones.db.url"))
          .set("hibernate.connection.username",
              env.getRequiredProperty("notificaciones.db.username"))
          .set("hibernate.connection.password",
              env.getProperty("notificaciones.db.password", ""))
          .set("hibernate.dialect",
              env.getRequiredProperty("notificaciones.db.dialect"))
          .set("hibernate.hbm2ddl.auto",
              env.getProperty("notificaciones.db.hbm2ddl", "update"))
          .set("hibernate.show_sql",
              env.getProperty("notificaciones.db.show-sql", "false")));
      // jpa-extras crea el EntityManagerFactory de forma lazy: se fuerza aca para que
      // hbm2ddl cree las tablas al arrancar, y se libera el EntityManager de este hilo
      WithSimplePersistenceUnit.PER_THREAD_ENTITY_MANAGER_ACCESS.get();
      WithSimplePersistenceUnit.dispose();
    } catch (IllegalStateException e) {
      // La unidad de persistencia ya fue inicializada.
    }
  }
}