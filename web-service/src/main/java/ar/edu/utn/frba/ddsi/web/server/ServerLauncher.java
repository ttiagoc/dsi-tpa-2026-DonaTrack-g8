package ar.edu.utn.frba.ddsi.web.server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Levanta el servidor al arrancar Spring; en los tests (perfil "test") cada test levanta el suyo. */
@Component
@Profile("!test")
public class ServerLauncher {

    private final Server server;

    @Value("${server.port:7070}")
    private int puerto;

    public ServerLauncher(Server server) {
        this.server = server;
    }

    @PostConstruct
    public void iniciar() {
        server.iniciar(puerto);
        System.out.println("web-service escuchando en http://localhost:" + puerto);
    }

    @PreDestroy
    public void detener() {
        server.detener();
    }
}
