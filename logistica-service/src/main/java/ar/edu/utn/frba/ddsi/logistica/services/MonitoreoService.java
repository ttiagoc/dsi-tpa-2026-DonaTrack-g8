package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.CamionActivoResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.monitoreo.UbicacionResponse;

public interface MonitoreoService {

    void actualizarUbicacionCamion(String patente, UbicacionRequest request);

    UbicacionResponse obtenerUltimaUbicacionPorRuta(Long rutaId);

    List<CamionActivoResponse> obtenerCamionesActivos();
}
