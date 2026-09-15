package ar.edu.utn.frba.ddsi.logistica.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Configura la conexion de la unidad de persistencia (persistence.xml) a partir de
 * application.properties: PostgreSQL para el despliegue local, HSQLDB en los tests.
 * El EntityManagerFactory se crea recien en el primer acceso, despues de esta configuracion.
 */
@Configuration
public class PersistenceConfig {

    public PersistenceConfig(Environment env) {
        try {
            WithSimplePersistenceUnit.configure(properties -> properties
                    .set("hibernate.connection.driver_class", env.getRequiredProperty("logistica.db.driver"))
                    .set("hibernate.connection.url", env.getRequiredProperty("logistica.db.url"))
                    .set("hibernate.connection.username", env.getRequiredProperty("logistica.db.username"))
                    .set("hibernate.connection.password", env.getProperty("logistica.db.password", ""))
                    .set("hibernate.dialect", env.getRequiredProperty("logistica.db.dialect"))
                    .set("hibernate.hbm2ddl.auto", env.getProperty("logistica.db.hbm2ddl", "update"))
                    .set("hibernate.show_sql", env.getProperty("logistica.db.show-sql", "false")));
        } catch (IllegalStateException e) {
            // La unidad de persistencia ya fue inicializada (por ejemplo, por otro test en la misma JVM)
        }
    }
}
