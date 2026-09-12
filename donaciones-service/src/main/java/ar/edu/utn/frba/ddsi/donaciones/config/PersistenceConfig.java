package ar.edu.utn.frba.ddsi.donaciones.config;

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
                    .set("hibernate.connection.driver_class", env.getRequiredProperty("donaciones.db.driver"))
                    .set("hibernate.connection.url", env.getRequiredProperty("donaciones.db.url"))
                    .set("hibernate.connection.username", env.getRequiredProperty("donaciones.db.username"))
                    .set("hibernate.connection.password", env.getProperty("donaciones.db.password", ""))
                    .set("hibernate.dialect", env.getRequiredProperty("donaciones.db.dialect"))
                    .set("hibernate.hbm2ddl.auto", env.getProperty("donaciones.db.hbm2ddl", "update"))
                    .set("hibernate.show_sql", env.getProperty("donaciones.db.show-sql", "false")));
        } catch (IllegalStateException e) {
            // La unidad de persistencia ya fue inicializada (por ejemplo, por otro test en la misma JVM)
        }
    }
}
