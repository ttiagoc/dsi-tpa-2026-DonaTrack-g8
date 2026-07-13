package ar.edu.utn.frba.ddsi.logistica.services;

import java.time.LocalDate;
import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.ruta.RutaResponse;

public interface RutaService {

    List<RutaResponse> obtenerTodas(LocalDate fecha);

    RutaResponse obtenerPorId(Long id);

    boolean eliminar(Long id);

    RutaResponse crear(RutaRequest request);

    RutaResponse actualizar(Long id, RutaRequest request);

    void actualizarEstado(Long id, String estado);

    void confirmarEntregaExitosa(Long rutaId, Long paradaId);

    UbicacionResponse obtenerUbicacionActual(Long id);
}
