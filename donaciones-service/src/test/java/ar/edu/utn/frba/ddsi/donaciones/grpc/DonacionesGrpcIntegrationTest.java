package ar.edu.utn.frba.ddsi.donaciones.grpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.security.ApiKeyGrpcServerInterceptor;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import ar.edu.utn.frba.ddsi.grpc.*;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

@DisplayName("Test de Integración gRPC: DonacionesGrpcService con Seguridad")
class DonacionesGrpcIntegrationTest {

    private static final String TEST_API_KEY = "test-secret-key-123";

    private Server server;
    private ManagedChannel channel;

    private DonacionService donacionService;
    private DonanteService donanteService;
    private EntidadBeneficiariaService entidadBeneficiariaService;

    @BeforeEach
    void setUp() throws IOException {
        donacionService = mock(DonacionService.class);
        donanteService = mock(DonanteService.class);
        entidadBeneficiariaService = mock(EntidadBeneficiariaService.class);

        ApiKeyGrpcServerInterceptor interceptor = new ApiKeyGrpcServerInterceptor();
        ReflectionTestUtils.setField(interceptor, "expectedApiKey", TEST_API_KEY);

        DonacionesGrpcServiceImpl serviceImpl = new DonacionesGrpcServiceImpl(
                donacionService,
                donanteService,
                entidadBeneficiariaService);

        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(serviceImpl, interceptor))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    @DisplayName("Debe rechazar llamada con UNAUTHENTICATED si no se envía x-api-key")
    void llamadaSinApiKeyRechazada() {
        DonacionesGrpcServiceGrpc.DonacionesGrpcServiceBlockingStub stub =
                DonacionesGrpcServiceGrpc.newBlockingStub(channel);

        ListarDonacionesPorEstadoRequest request = ListarDonacionesPorEstadoRequest.newBuilder()
                .setEstado("ASIGNADA")
                .setLimit(10)
                .setOffset(0)
                .build();

        StatusRuntimeException ex = assertThrows(StatusRuntimeException.class, () -> {
            stub.listarDonacionesPorEstado(request);
        });

        assertEquals(Status.Code.UNAUTHENTICATED, ex.getStatus().getCode());
    }

    @Test
    @DisplayName("Debe procesar llamada exitosamente con x-api-key válida")
    void llamadaConApiKeyValidaExitosa() {
        ClientInterceptor clientAuthInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        headers.put(ApiKeyGrpcServerInterceptor.API_KEY_METADATA_KEY, TEST_API_KEY);
                        super.start(responseListener, headers);
                    }
                };
            }
        };

        DonacionesGrpcServiceGrpc.DonacionesGrpcServiceBlockingStub stub =
                DonacionesGrpcServiceGrpc.newBlockingStub(channel).withInterceptors(clientAuthInterceptor);

        when(donacionService.obtenerDonacionesSegunEstado("ASIGNADA", 10, 0))
                .thenReturn(List.of(new DonacionAsignadaResponse(1L, 10.5, 0.5, "Av. Siempre Viva 742")));

        ListarDonacionesPorEstadoRequest request = ListarDonacionesPorEstadoRequest.newBuilder()
                .setEstado("ASIGNADA")
                .setLimit(10)
                .setOffset(0)
                .build();

        ListarDonacionesPorEstadoResponse response = stub.listarDonacionesPorEstado(request);

        assertNotNull(response);
        assertEquals(1, response.getDonacionesCount());
        assertEquals(1L, response.getDonaciones(0).getId());
        assertEquals("Av. Siempre Viva 742", response.getDonaciones(0).getDireccion());
    }

    @Test
    @DisplayName("Debe actualizar estado correctamente vía gRPC con x-api-key")
    void actualizarEstadoExitoso() {
        ClientInterceptor clientAuthInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        headers.put(ApiKeyGrpcServerInterceptor.API_KEY_METADATA_KEY, TEST_API_KEY);
                        super.start(responseListener, headers);
                    }
                };
            }
        };

        DonacionesGrpcServiceGrpc.DonacionesGrpcServiceBlockingStub stub =
                DonacionesGrpcServiceGrpc.newBlockingStub(channel).withInterceptors(clientAuthInterceptor);

        when(donacionService.cambiarEstado(eq(5L), any()))
                .thenReturn(new EstadoDonacionResponse(5L, "LISTA_PARA_ENTREGAR", LocalDateTime.now()));

        ActualizarEstadoDonacionRequest request = ActualizarEstadoDonacionRequest.newBuilder()
                .setDonacionId(5L)
                .setNuevoEstado("LISTA_PARA_ENTREGAR")
                .setJustificacion("Listo")
                .build();

        ActualizarEstadoDonacionResponse response = stub.actualizarEstadoDonacion(request);

        assertNotNull(response);
        assertTrue(response.getExito());
        verify(donacionService, times(1)).cambiarEstado(eq(5L), any());
    }
}

