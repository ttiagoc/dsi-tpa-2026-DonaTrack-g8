package ar.edu.utn.frba.ddsi.donaciones.grpc;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.security.ApiKeyGrpcServerInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servidor gRPC que escucha peticiones en un puerto dedicado (por defecto 9090).
 * Sigue el mismo patrón de ciclo de vida que JavalinWebServer en el proyecto:
 * arranca en @PostConstruct y se detiene en @PreDestroy.
 * Solo se activa fuera del perfil 'test' para no interferir con pruebas de persistencia.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DonacionesGrpcServer {

    @Value("${grpc.server.port:9090}")
    private int port;

    private final DonacionesGrpcServiceImpl donacionesGrpcService;
    private final ApiKeyGrpcServerInterceptor apiKeyInterceptor;

    private Server server;

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(donacionesGrpcService, apiKeyInterceptor))
                .build()
                .start();

        log.info("Servidor gRPC Donaciones iniciado en el puerto {}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Deteniendo servidor gRPC Donaciones...");
            server.shutdown();
        }
    }
}

