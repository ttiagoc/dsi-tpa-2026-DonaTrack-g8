package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CamionResponse;

public interface CamionService {

    List<CamionResponse> obtenerTodos();

    CamionResponse obtenerPorId(Long id);

    CamionResponse crear(CamionRequest request);

    CamionResponse actualizar(Long id, CamionRequest request);

    boolean eliminar(Long id);
}
