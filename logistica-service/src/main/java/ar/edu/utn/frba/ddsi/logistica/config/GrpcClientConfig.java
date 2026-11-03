package ar.edu.utn.frba.ddsi.logistica.config;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ar.edu.utn.frba.ddsi.grpc.DonacionesGrpcServiceGrpc;
import ar.edu.utn.frba.ddsi.grpc.DonacionesGrpcServiceGrpc.DonacionesGrpcServiceBlockingStub;
import ar.edu.utn.frba.ddsi.logistica.grpc.ApiKeyGrpcClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuración del canal y stub gRPC hacia donaciones-service.
 */
@Configuration
@Slf4j
public class GrpcClientConfig {

    @Value("${grpc.donaciones.host:localhost}")
    private String host;

    @Value("${grpc.donaciones.port:9090}")
    private int port;

    private ManagedChannel channel;

    @Bean
    public ManagedChannel donacionesManagedChannel() {
        log.info("Creando canal gRPC hacia donaciones-service en {}:{}", host, port);
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        return this.channel;
    }

    @Bean
    public DonacionesGrpcServiceBlockingStub donacionesGrpcBlockingStub(
            ManagedChannel channel,
            ApiKeyGrpcClientInterceptor apiKeyInterceptor) {
        return DonacionesGrpcServiceGrpc.newBlockingStub(channel)
                .withInterceptors(apiKeyInterceptor);
    }

    @PreDestroy
    public void cleanup() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Cerrando canal gRPC hacia donaciones-service...");
            channel.shutdown();
        }
    }
}

