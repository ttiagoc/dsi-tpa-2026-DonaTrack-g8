package ar.edu.utn.frba.ddsi.donaciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.services.AuthService;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Los administradores no se registran desde la web: el primero se crea al arrancar con
 * ADMIN_USERNAME / ADMIN_PASSWORD (ver application.properties).
 */
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private final AuthService authService;
    private final String username;
    private final String password;

    public InicializadorAdmin(AuthService authService,
            @Value("${seguridad.admin.username}") String username,
            @Value("${seguridad.admin.password}") String password) {
        this.authService = authService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            authService.crearAdministradorSiNoExiste(username, password);
        } finally {
            // se libera el EntityManager del hilo de arranque, como hace PersistenceConfig
            WithSimplePersistenceUnit.dispose();
        }
    }
}
