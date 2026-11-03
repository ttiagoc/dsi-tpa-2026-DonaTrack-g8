package ar.edu.utn.frba.ddsi.logistica.grpc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.grpc.*;
import ar.edu.utn.frba.ddsi.grpc.DonacionesGrpcServiceGrpc.DonacionesGrpcServiceBlockingStub;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de alto nivel para invocar a donaciones-service vía gRPC.
 * Adapta los objetos de dominio de logística con los mensajes de Protobuf.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DonacionesGrpcClient {

    private final DonacionesGrpcServiceBlockingStub stub;

    public List<DonacionDTO> listarPorEstado(String estado, int limit, int offset) {
        try {
            ListarDonacionesPorEstadoRequest request = ListarDonacionesPorEstadoRequest.newBuilder()
                    .setEstado(estado)
                    .setLimit(limit)
                    .setOffset(offset)
                    .build();

            ListarDonacionesPorEstadoResponse response = stub.listarDonacionesPorEstado(request);

            List<DonacionDTO> dtos = new ArrayList<>();
            for (DonacionItem item : response.getDonacionesList()) {
                dtos.add(new DonacionDTO(
                        item.getId(),
                        item.getPeso(),
                        item.getVolumen(),
                        item.getDireccion()));
            }
            return dtos;
        } catch (Exception e) {
            log.error("Error gRPC al listar donaciones por estado '{}': {}", estado, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void actualizarEstado(Long donacionId, String nuevoEstado, String justificacion) {
        try {
            ActualizarEstadoDonacionRequest request = ActualizarEstadoDonacionRequest.newBuilder()
                    .setDonacionId(donacionId)
                    .setNuevoEstado(nuevoEstado)
                    .setJustificacion(justificacion != null ? justificacion : "")
                    .build();

            stub.actualizarEstadoDonacion(request);
        } catch (Exception e) {
            log.error("Error gRPC al actualizar estado de donación {}: {}", donacionId, e.getMessage());
        }
    }

    public void confirmarEntrega(Long entidadId, List<Long> donacionesIds, String patente, LocalDateTime fechaHora) {
        try {
            ConfirmarEntregaDonacionesRequest request = ConfirmarEntregaDonacionesRequest.newBuilder()
                    .setEntidadId(entidadId != null ? entidadId : 0L)
                    .addAllDonacionIds(donacionesIds != null ? donacionesIds : Collections.emptyList())
                    .setPatenteCamion(patente != null ? patente : "")
                    .setFechaHora(fechaHora != null ? fechaHora.toString() : "")
                    .build();

            stub.confirmarEntregaDonaciones(request);
        } catch (Exception e) {
            log.error("Error gRPC al confirmar entrega exitosa para entidad {}: {}", entidadId, e.getMessage());
        }
    }

    public List<MedioContacto> obtenerContactosEntidad(Long entidadId) {
        try {
            ObtenerContactosEntidadRequest request = ObtenerContactosEntidadRequest.newBuilder()
                    .setEntidadId(entidadId)
                    .build();

            ObtenerContactosEntidadResponse response = stub.obtenerContactosEntidad(request);

            List<MedioContacto> contactos = new ArrayList<>();
            for (ContactoItem item : response.getContactosList()) {
                TipoContacto tipo = null;
                if (!item.getTipoContacto().isBlank()) {
                    try {
                        tipo = TipoContacto.valueOf(item.getTipoContacto());
                    } catch (IllegalArgumentException ignored) {}
                }
                contactos.add(new MedioContacto(item.getValor(), tipo));
            }
            return contactos;
        } catch (Exception e) {
            log.error("Error gRPC al obtener contactos de entidad {}: {}", entidadId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public DonacionResponse obtenerDonacion(Long donacionId) {
        try {
            ObtenerDonacionRequest request = ObtenerDonacionRequest.newBuilder()
                    .setDonacionId(donacionId)
                    .build();

            DonacionDetalleResponse response = stub.obtenerDonacion(request);
            return new DonacionResponse(response.getId(), response.getDonanteId());
        } catch (Exception e) {
            log.error("Error gRPC al obtener donación {}: {}", donacionId, e.getMessage());
            return null;
        }
    }

    public MedioContacto obtenerContactoDonante(Long donanteId) {
        try {
            ObtenerContactoDonanteRequest request = ObtenerContactoDonanteRequest.newBuilder()
                    .setDonanteId(donanteId)
                    .build();

            ContactoDonanteResponse response = stub.obtenerContactoDonante(request);
            TipoContacto tipo = null;
            if (!response.getTipoContacto().isBlank()) {
                try {
                    tipo = TipoContacto.valueOf(response.getTipoContacto());
                } catch (IllegalArgumentException ignored) {}
            }
            return new MedioContacto(response.getValor(), tipo);
        } catch (Exception e) {
            log.error("Error gRPC al obtener contacto de donante {}: {}", donanteId, e.getMessage());
            return null;
        }
    }
}

