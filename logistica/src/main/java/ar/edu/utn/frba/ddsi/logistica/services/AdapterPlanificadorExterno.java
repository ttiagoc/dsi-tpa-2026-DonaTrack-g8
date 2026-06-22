package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;

public interface AdapterPlanificadorExterno {
    void solicitarPlanificacionAsync(List<DonacionDTO> lote, List<Camion> camiones);
}
