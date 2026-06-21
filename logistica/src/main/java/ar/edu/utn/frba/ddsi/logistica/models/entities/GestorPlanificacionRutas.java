package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.List;
import lombok.Data;

@Data
public class GestorPlanificacionRutas {
    private AdapterPlanificadorExterno adapter;

    public void planificarEntregasDelDiaSiguiente(List<DonacionDTO> donacionesDisponibles, List<Camion> camiones) {
    }

    private List<List<DonacionDTO>> fraccionarEnLotesDeCien(List<DonacionDTO> donaciones) {
        return null;
    }
}
