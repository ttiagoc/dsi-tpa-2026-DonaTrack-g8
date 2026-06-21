package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.List;

public interface AdapterPlanificadorExterno {
    void solicitarPlanificacionAsync(List<DonacionDTO> lote, List<Camion> camiones);
}
