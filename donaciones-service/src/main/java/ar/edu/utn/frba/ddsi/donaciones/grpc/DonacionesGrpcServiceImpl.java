package ar.edu.utn.frba.ddsi.donaciones.grpc;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionAsignadaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import ar.edu.utn.frba.ddsi.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio gRPC DonacionesGrpcService.
 * Atiende las invocaciones de logistica-service delegando en los servicios existentes de Spring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonacionesGrpcServiceImpl extends DonacionesGrpcServiceGrpc.DonacionesGrpcServiceImplBase {

    private final DonacionService donacionService;
    private final DonanteService donanteService;
    private final EntidadBeneficiariaService entidadBeneficiariaService;

    @Override
    public void listarDonacionesPorEstado(
            ListarDonacionesPorEstadoRequest request,
            StreamObserver<ListarDonacionesPorEstadoResponse> responseObserver) {
        try {
            List<DonacionAsignadaResponse> donaciones = donacionService.obtenerDonacionesSegunEstado(
                    request.getEstado(),
                    request.getLimit(),
                    request.getOffset());

            ListarDonacionesPorEstadoResponse.Builder builder = ListarDonacionesPorEstadoResponse.newBuilder();
            for (DonacionAsignadaResponse d : donaciones) {
                boolean tieneUbicacion = d.latitud() != null && d.longitud() != null;
                builder.addDonaciones(DonacionItem.newBuilder()
                        .setId(d.id() != null ? d.id() : 0L)
                        .setPeso(d.peso() != null ? d.peso() : 0.0)
                        .setVolumen(d.volumen() != null ? d.volumen() : 0.0)
                        .setDireccion(d.direccion() != null ? d.direccion() : "")
                        .setEntidadId(d.entidadId() != null ? d.entidadId() : 0L)
                        .setEntidadNombre(d.entidadNombre() != null ? d.entidadNombre() : "")
                        .setTieneUbicacion(tieneUbicacion)
                        .setLatitud(tieneUbicacion ? d.latitud() : 0.0)
                        .setLongitud(tieneUbicacion ? d.longitud() : 0.0)
                        .build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error gRPC en listarDonacionesPorEstado", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void actualizarEstadoDonacion(
            ActualizarEstadoDonacionRequest request,
            StreamObserver<ActualizarEstadoDonacionResponse> responseObserver) {
        try {
            EstadoDonacionRequest dto = new EstadoDonacionRequest(
                    request.getNuevoEstado(),
                    request.getJustificacion());

            donacionService.cambiarEstado(request.getDonacionId(), dto);

            responseObserver.onNext(ActualizarEstadoDonacionResponse.newBuilder()
                    .setExito(true)
                    .setMensaje("Estado actualizado correctamente")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error gRPC en actualizarEstadoDonacion id={}", request.getDonacionId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void confirmarEntregaDonaciones(
            ConfirmarEntregaDonacionesRequest request,
            StreamObserver<ConfirmarEntregaDonacionesResponse> responseObserver) {
        try {
            LocalDateTime fechaHora = LocalDateTime.now();
            if (!request.getFechaHora().isBlank()) {
                try {
                    fechaHora = LocalDateTime.parse(request.getFechaHora());
                } catch (DateTimeParseException ignored) {
                    // Si falla el parseo, se mantiene la fecha actual
                }
            }

            ConfirmacionEntregaExitosaRequest dto = new ConfirmacionEntregaExitosaRequest(
                    request.getEntidadId(),
                    request.getDonacionIdsList(),
                    request.getPatenteCamion(),
                    fechaHora);

            donacionService.confirmarEntregaExitosa(dto);

            responseObserver.onNext(ConfirmarEntregaDonacionesResponse.newBuilder()
                    .setExito(true)
                    .setMensaje("Entrega confirmada exitosamente")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error gRPC en confirmarEntregaDonaciones", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void obtenerContactosEntidad(
            ObtenerContactosEntidadRequest request,
            StreamObserver<ObtenerContactosEntidadResponse> responseObserver) {
        try {
            List<MedioContacto> contactos = entidadBeneficiariaService.obtenerContactos(request.getEntidadId());

            ObtenerContactosEntidadResponse.Builder builder = ObtenerContactosEntidadResponse.newBuilder();
            if (contactos != null) {
                for (MedioContacto c : contactos) {
                    builder.addContactos(ContactoItem.newBuilder()
                            .setValor(c.getValor() != null ? c.getValor() : "")
                            .setTipoContacto(c.getTipoContacto() != null ? c.getTipoContacto().name() : "")
                            .build());
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error gRPC en obtenerContactosEntidad id={}", request.getEntidadId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void obtenerDonacion(
            ObtenerDonacionRequest request,
            StreamObserver<DonacionDetalleResponse> responseObserver) {
        try {
            DonacionResponse donacion = donacionService.obtenerPorId(request.getDonacionId());

            if (donacion == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("Donación no encontrada").asRuntimeException());
                return;
            }

            DonacionDetalleResponse response = DonacionDetalleResponse.newBuilder()
                    .setId(donacion.id() != null ? donacion.id() : 0L)
                    .setDonanteId(donacion.donanteId() != null ? donacion.donanteId() : 0L)
                    .setEntidadAsignadaId(donacion.entidadAsignadaId() != null ? donacion.entidadAsignadaId() : 0L)
                    .setEstado(donacion.estado() != null ? donacion.estado() : "")
                    .setDescripcion(donacion.descripcion() != null ? donacion.descripcion() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error gRPC en obtenerDonacion id={}", request.getDonacionId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void obtenerContactoDonante(
            ObtenerContactoDonanteRequest request,
            StreamObserver<ContactoDonanteResponse> responseObserver) {
        try {
            MedioContacto contacto = donanteService.obtenerContactoPredeterminado(request.getDonanteId());

            ContactoDonanteResponse.Builder builder = ContactoDonanteResponse.newBuilder();
            if (contacto != null) {
                builder.setValor(contacto.getValor() != null ? contacto.getValor() : "");
                builder.setTipoContacto(contacto.getTipoContacto() != null ? contacto.getTipoContacto().name() : "");
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error gRPC en obtenerContactoDonante id={}", request.getDonanteId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}

