package ar.edu.utn.frba.ddsi.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: donaciones.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class DonacionesGrpcServiceGrpc {

  private DonacionesGrpcServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ar.edu.utn.frba.ddsi.grpc.DonacionesGrpcService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest,
      ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse> getListarDonacionesPorEstadoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListarDonacionesPorEstado",
      requestType = ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest.class,
      responseType = ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest,
      ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse> getListarDonacionesPorEstadoMethod() {
    io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest, ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse> getListarDonacionesPorEstadoMethod;
    if ((getListarDonacionesPorEstadoMethod = DonacionesGrpcServiceGrpc.getListarDonacionesPorEstadoMethod) == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        if ((getListarDonacionesPorEstadoMethod = DonacionesGrpcServiceGrpc.getListarDonacionesPorEstadoMethod) == null) {
          DonacionesGrpcServiceGrpc.getListarDonacionesPorEstadoMethod = getListarDonacionesPorEstadoMethod =
              io.grpc.MethodDescriptor.<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest, ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListarDonacionesPorEstado"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DonacionesGrpcServiceMethodDescriptorSupplier("ListarDonacionesPorEstado"))
              .build();
        }
      }
    }
    return getListarDonacionesPorEstadoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest,
      ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse> getActualizarEstadoDonacionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ActualizarEstadoDonacion",
      requestType = ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest.class,
      responseType = ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest,
      ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse> getActualizarEstadoDonacionMethod() {
    io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest, ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse> getActualizarEstadoDonacionMethod;
    if ((getActualizarEstadoDonacionMethod = DonacionesGrpcServiceGrpc.getActualizarEstadoDonacionMethod) == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        if ((getActualizarEstadoDonacionMethod = DonacionesGrpcServiceGrpc.getActualizarEstadoDonacionMethod) == null) {
          DonacionesGrpcServiceGrpc.getActualizarEstadoDonacionMethod = getActualizarEstadoDonacionMethod =
              io.grpc.MethodDescriptor.<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest, ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ActualizarEstadoDonacion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DonacionesGrpcServiceMethodDescriptorSupplier("ActualizarEstadoDonacion"))
              .build();
        }
      }
    }
    return getActualizarEstadoDonacionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest,
      ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse> getConfirmarEntregaDonacionesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ConfirmarEntregaDonaciones",
      requestType = ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest.class,
      responseType = ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest,
      ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse> getConfirmarEntregaDonacionesMethod() {
    io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest, ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse> getConfirmarEntregaDonacionesMethod;
    if ((getConfirmarEntregaDonacionesMethod = DonacionesGrpcServiceGrpc.getConfirmarEntregaDonacionesMethod) == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        if ((getConfirmarEntregaDonacionesMethod = DonacionesGrpcServiceGrpc.getConfirmarEntregaDonacionesMethod) == null) {
          DonacionesGrpcServiceGrpc.getConfirmarEntregaDonacionesMethod = getConfirmarEntregaDonacionesMethod =
              io.grpc.MethodDescriptor.<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest, ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ConfirmarEntregaDonaciones"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DonacionesGrpcServiceMethodDescriptorSupplier("ConfirmarEntregaDonaciones"))
              .build();
        }
      }
    }
    return getConfirmarEntregaDonacionesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest,
      ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse> getObtenerContactosEntidadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ObtenerContactosEntidad",
      requestType = ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest.class,
      responseType = ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest,
      ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse> getObtenerContactosEntidadMethod() {
    io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest, ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse> getObtenerContactosEntidadMethod;
    if ((getObtenerContactosEntidadMethod = DonacionesGrpcServiceGrpc.getObtenerContactosEntidadMethod) == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        if ((getObtenerContactosEntidadMethod = DonacionesGrpcServiceGrpc.getObtenerContactosEntidadMethod) == null) {
          DonacionesGrpcServiceGrpc.getObtenerContactosEntidadMethod = getObtenerContactosEntidadMethod =
              io.grpc.MethodDescriptor.<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest, ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ObtenerContactosEntidad"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DonacionesGrpcServiceMethodDescriptorSupplier("ObtenerContactosEntidad"))
              .build();
        }
      }
    }
    return getObtenerContactosEntidadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest,
      ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse> getObtenerDonacionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ObtenerDonacion",
      requestType = ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest.class,
      responseType = ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest,
      ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse> getObtenerDonacionMethod() {
    io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest, ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse> getObtenerDonacionMethod;
    if ((getObtenerDonacionMethod = DonacionesGrpcServiceGrpc.getObtenerDonacionMethod) == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        if ((getObtenerDonacionMethod = DonacionesGrpcServiceGrpc.getObtenerDonacionMethod) == null) {
          DonacionesGrpcServiceGrpc.getObtenerDonacionMethod = getObtenerDonacionMethod =
              io.grpc.MethodDescriptor.<ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest, ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ObtenerDonacion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DonacionesGrpcServiceMethodDescriptorSupplier("ObtenerDonacion"))
              .build();
        }
      }
    }
    return getObtenerDonacionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest,
      ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse> getObtenerContactoDonanteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ObtenerContactoDonante",
      requestType = ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest.class,
      responseType = ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest,
      ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse> getObtenerContactoDonanteMethod() {
    io.grpc.MethodDescriptor<ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest, ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse> getObtenerContactoDonanteMethod;
    if ((getObtenerContactoDonanteMethod = DonacionesGrpcServiceGrpc.getObtenerContactoDonanteMethod) == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        if ((getObtenerContactoDonanteMethod = DonacionesGrpcServiceGrpc.getObtenerContactoDonanteMethod) == null) {
          DonacionesGrpcServiceGrpc.getObtenerContactoDonanteMethod = getObtenerContactoDonanteMethod =
              io.grpc.MethodDescriptor.<ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest, ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ObtenerContactoDonante"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DonacionesGrpcServiceMethodDescriptorSupplier("ObtenerContactoDonante"))
              .build();
        }
      }
    }
    return getObtenerContactoDonanteMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DonacionesGrpcServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DonacionesGrpcServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DonacionesGrpcServiceStub>() {
        @java.lang.Override
        public DonacionesGrpcServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DonacionesGrpcServiceStub(channel, callOptions);
        }
      };
    return DonacionesGrpcServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DonacionesGrpcServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DonacionesGrpcServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DonacionesGrpcServiceBlockingStub>() {
        @java.lang.Override
        public DonacionesGrpcServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DonacionesGrpcServiceBlockingStub(channel, callOptions);
        }
      };
    return DonacionesGrpcServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DonacionesGrpcServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DonacionesGrpcServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DonacionesGrpcServiceFutureStub>() {
        @java.lang.Override
        public DonacionesGrpcServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DonacionesGrpcServiceFutureStub(channel, callOptions);
        }
      };
    return DonacionesGrpcServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void listarDonacionesPorEstado(ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListarDonacionesPorEstadoMethod(), responseObserver);
    }

    /**
     */
    default void actualizarEstadoDonacion(ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getActualizarEstadoDonacionMethod(), responseObserver);
    }

    /**
     */
    default void confirmarEntregaDonaciones(ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getConfirmarEntregaDonacionesMethod(), responseObserver);
    }

    /**
     */
    default void obtenerContactosEntidad(ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getObtenerContactosEntidadMethod(), responseObserver);
    }

    /**
     */
    default void obtenerDonacion(ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getObtenerDonacionMethod(), responseObserver);
    }

    /**
     */
    default void obtenerContactoDonante(ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getObtenerContactoDonanteMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service DonacionesGrpcService.
   */
  public static abstract class DonacionesGrpcServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return DonacionesGrpcServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service DonacionesGrpcService.
   */
  public static final class DonacionesGrpcServiceStub
      extends io.grpc.stub.AbstractAsyncStub<DonacionesGrpcServiceStub> {
    private DonacionesGrpcServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DonacionesGrpcServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DonacionesGrpcServiceStub(channel, callOptions);
    }

    /**
     */
    public void listarDonacionesPorEstado(ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListarDonacionesPorEstadoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void actualizarEstadoDonacion(ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getActualizarEstadoDonacionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void confirmarEntregaDonaciones(ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getConfirmarEntregaDonacionesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void obtenerContactosEntidad(ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getObtenerContactosEntidadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void obtenerDonacion(ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getObtenerDonacionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void obtenerContactoDonante(ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest request,
        io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getObtenerContactoDonanteMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service DonacionesGrpcService.
   */
  public static final class DonacionesGrpcServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<DonacionesGrpcServiceBlockingStub> {
    private DonacionesGrpcServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DonacionesGrpcServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DonacionesGrpcServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse listarDonacionesPorEstado(ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListarDonacionesPorEstadoMethod(), getCallOptions(), request);
    }

    /**
     */
    public ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse actualizarEstadoDonacion(ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getActualizarEstadoDonacionMethod(), getCallOptions(), request);
    }

    /**
     */
    public ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse confirmarEntregaDonaciones(ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getConfirmarEntregaDonacionesMethod(), getCallOptions(), request);
    }

    /**
     */
    public ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse obtenerContactosEntidad(ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getObtenerContactosEntidadMethod(), getCallOptions(), request);
    }

    /**
     */
    public ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse obtenerDonacion(ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getObtenerDonacionMethod(), getCallOptions(), request);
    }

    /**
     */
    public ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse obtenerContactoDonante(ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getObtenerContactoDonanteMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service DonacionesGrpcService.
   */
  public static final class DonacionesGrpcServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<DonacionesGrpcServiceFutureStub> {
    private DonacionesGrpcServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DonacionesGrpcServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DonacionesGrpcServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse> listarDonacionesPorEstado(
        ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListarDonacionesPorEstadoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse> actualizarEstadoDonacion(
        ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getActualizarEstadoDonacionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse> confirmarEntregaDonaciones(
        ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getConfirmarEntregaDonacionesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse> obtenerContactosEntidad(
        ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getObtenerContactosEntidadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse> obtenerDonacion(
        ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getObtenerDonacionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse> obtenerContactoDonante(
        ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getObtenerContactoDonanteMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LISTAR_DONACIONES_POR_ESTADO = 0;
  private static final int METHODID_ACTUALIZAR_ESTADO_DONACION = 1;
  private static final int METHODID_CONFIRMAR_ENTREGA_DONACIONES = 2;
  private static final int METHODID_OBTENER_CONTACTOS_ENTIDAD = 3;
  private static final int METHODID_OBTENER_DONACION = 4;
  private static final int METHODID_OBTENER_CONTACTO_DONANTE = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LISTAR_DONACIONES_POR_ESTADO:
          serviceImpl.listarDonacionesPorEstado((ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest) request,
              (io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse>) responseObserver);
          break;
        case METHODID_ACTUALIZAR_ESTADO_DONACION:
          serviceImpl.actualizarEstadoDonacion((ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest) request,
              (io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse>) responseObserver);
          break;
        case METHODID_CONFIRMAR_ENTREGA_DONACIONES:
          serviceImpl.confirmarEntregaDonaciones((ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest) request,
              (io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse>) responseObserver);
          break;
        case METHODID_OBTENER_CONTACTOS_ENTIDAD:
          serviceImpl.obtenerContactosEntidad((ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest) request,
              (io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse>) responseObserver);
          break;
        case METHODID_OBTENER_DONACION:
          serviceImpl.obtenerDonacion((ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest) request,
              (io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse>) responseObserver);
          break;
        case METHODID_OBTENER_CONTACTO_DONANTE:
          serviceImpl.obtenerContactoDonante((ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest) request,
              (io.grpc.stub.StreamObserver<ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getListarDonacionesPorEstadoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoRequest,
              ar.edu.utn.frba.ddsi.grpc.ListarDonacionesPorEstadoResponse>(
                service, METHODID_LISTAR_DONACIONES_POR_ESTADO)))
        .addMethod(
          getActualizarEstadoDonacionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionRequest,
              ar.edu.utn.frba.ddsi.grpc.ActualizarEstadoDonacionResponse>(
                service, METHODID_ACTUALIZAR_ESTADO_DONACION)))
        .addMethod(
          getConfirmarEntregaDonacionesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesRequest,
              ar.edu.utn.frba.ddsi.grpc.ConfirmarEntregaDonacionesResponse>(
                service, METHODID_CONFIRMAR_ENTREGA_DONACIONES)))
        .addMethod(
          getObtenerContactosEntidadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadRequest,
              ar.edu.utn.frba.ddsi.grpc.ObtenerContactosEntidadResponse>(
                service, METHODID_OBTENER_CONTACTOS_ENTIDAD)))
        .addMethod(
          getObtenerDonacionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ar.edu.utn.frba.ddsi.grpc.ObtenerDonacionRequest,
              ar.edu.utn.frba.ddsi.grpc.DonacionDetalleResponse>(
                service, METHODID_OBTENER_DONACION)))
        .addMethod(
          getObtenerContactoDonanteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              ar.edu.utn.frba.ddsi.grpc.ObtenerContactoDonanteRequest,
              ar.edu.utn.frba.ddsi.grpc.ContactoDonanteResponse>(
                service, METHODID_OBTENER_CONTACTO_DONANTE)))
        .build();
  }

  private static abstract class DonacionesGrpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DonacionesGrpcServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ar.edu.utn.frba.ddsi.grpc.DonacionesProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DonacionesGrpcService");
    }
  }

  private static final class DonacionesGrpcServiceFileDescriptorSupplier
      extends DonacionesGrpcServiceBaseDescriptorSupplier {
    DonacionesGrpcServiceFileDescriptorSupplier() {}
  }

  private static final class DonacionesGrpcServiceMethodDescriptorSupplier
      extends DonacionesGrpcServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    DonacionesGrpcServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DonacionesGrpcServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DonacionesGrpcServiceFileDescriptorSupplier())
              .addMethod(getListarDonacionesPorEstadoMethod())
              .addMethod(getActualizarEstadoDonacionMethod())
              .addMethod(getConfirmarEntregaDonacionesMethod())
              .addMethod(getObtenerContactosEntidadMethod())
              .addMethod(getObtenerDonacionMethod())
              .addMethod(getObtenerContactoDonanteMethod())
              .build();
        }
      }
    }
    return result;
  }
}
