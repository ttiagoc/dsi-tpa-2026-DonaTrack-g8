package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.dtos.pagina.PaginaResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;

public interface CamionService {

    List<CamionResponse> obtenerTodos();

    PaginaResponse<CamionResponse> obtenerTodosPaginado(int page, int size);

    CamionResponse obtenerPorId(Long id);

    CamionResponse crear(CamionRequest request);

    CamionResponse actualizar(Long id, CamionRequest request);

    boolean eliminar(Long id);

    List<CamionActivoResponse> obtenerCamionesActivos();

    void recibirTelemetria(String patente, UbicacionRequest request);
}
