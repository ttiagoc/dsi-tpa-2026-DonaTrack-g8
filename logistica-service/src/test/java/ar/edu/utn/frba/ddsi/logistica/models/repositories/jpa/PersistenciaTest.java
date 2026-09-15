package ar.edu.utn.frba.ddsi.logistica.models.repositories.jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Base de los tests de persistencia: usa HSQLDB en memoria y deja la base vacia antes de cada test.
 */
public abstract class PersistenciaTest implements WithSimplePersistenceUnit {

    static {
        try {
            WithSimplePersistenceUnit.configure(properties -> properties
                    .set("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver")
                    .set("hibernate.connection.url", "jdbc:hsqldb:mem:logistica-test")
                    .set("hibernate.connection.username", "sa")
                    .set("hibernate.connection.password", "")
                    .set("hibernate.dialect", "org.hibernate.dialect.HSQLDialect")
                    .set("hibernate.hbm2ddl.auto", "create-drop"));
        } catch (IllegalStateException yaInicializada) {
            // otro test ya inicializo la unidad de persistencia con esta misma configuracion
        }
    }

    @BeforeEach
    void limpiarBase() {
        withTransaction(() -> {
            entityManager()
                    .createNativeQuery("TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK")
                    .executeUpdate();
        });
        WithSimplePersistenceUnit.dispose();
    }

    @AfterEach
    void liberarEntityManager() {
        rollbackTransaction();
        WithSimplePersistenceUnit.dispose();
    }

    /** Simula un nuevo request: descarta las entidades cacheadas para forzar la lectura desde la base. */
    protected void nuevoRequest() {
        WithSimplePersistenceUnit.dispose();
    }
}
