package ar.edu.utn.frba.ddsi.notificaciones.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Configura la conexion de la unidad de persistencia (persistence.xml) a partir de
 * application.properties: PostgreSQL para el despliegue local, HSQLDB en los tests.
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
    } catch (IllegalStateException e) {
      // La unidad de persistencia ya fue inicializada.
    }
  }
}