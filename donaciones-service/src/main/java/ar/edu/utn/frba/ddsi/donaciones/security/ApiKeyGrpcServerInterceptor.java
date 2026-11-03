package ar.edu.utn.frba.ddsi.donaciones.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * Interceptor gRPC del lado servidor que valida el header 'x-api-key' en los metadatos.
 * Protege el canal gRPC entre microservicios exigiendo la clave compartida (${SERVICE_API_KEY}).
 */
@Component
public class ApiKeyGrpcServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> API_KEY_METADATA_KEY =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    @Value("${seguridad.service-api-key:clave-interna-de-servicio}")
    private String expectedApiKey;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String apiKey = headers.get(API_KEY_METADATA_KEY);

        if (expectedApiKey != null && !expectedApiKey.isBlank() && !expectedApiKey.equals(apiKey)) {
            call.close(
                    Status.UNAUTHENTICATED.withDescription("Acceso denegado: clave x-api-key ausente o invalida"),
                    new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }

        return next.startCall(call, headers);
    }
}

