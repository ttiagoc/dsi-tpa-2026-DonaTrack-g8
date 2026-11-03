package ar.edu.utn.frba.ddsi.logistica.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Interceptor gRPC del lado cliente que adjunta la clave compartida (x-api-key)
 * a los metadatos de cada llamada saliente hacia donaciones-service.
 */
@Component
public class ApiKeyGrpcClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> API_KEY_METADATA_KEY =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    @Value("${seguridad.service-api-key:donatrack-clave-servicios-local}")
    private String serviceApiKey;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (serviceApiKey != null && !serviceApiKey.isBlank()) {
                    headers.put(API_KEY_METADATA_KEY, serviceApiKey);
                }
                super.start(responseListener, headers);
            }
        };
    }
}

