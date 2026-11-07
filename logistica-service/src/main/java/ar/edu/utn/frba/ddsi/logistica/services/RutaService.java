package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;

public interface RutaService {

    List<RutaResponse> obtenerTodas(LocalDate fecha);

    PaginaResponse<RutaResponse> obtenerTodasPaginado(LocalDate fecha, int page, int size);

    RutaResponse obtenerPorId(Long id);

    Optional<RutaResponse> obtenerRutaActivaPorCamion(Long camionId);

    List<RutaResponse> obtenerPorCamion(Long camionId);

    boolean eliminar(Long id);

    RutaResponse crear(RutaRequest request);

    RutaResponse actualizar(Long id, RutaRequest request);

    void actualizarEstado(Long id, String estado);

    void confirmarEntregaExitosa(Long rutaId, Long paradaId);

    void registrarEntregaNoRecibida(Long rutaId, Long paradaId);

    void registrarEntregaNoRecibida(Long rutaId, Long paradaId, String motivo);

    UbicacionResponse obtenerUbicacionActual(Long id);

    List<ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaSeguimientoResponse> obtenerRutasActivas(Long entidadId, List<Long> donacionIds);
}
