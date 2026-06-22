package ar.edu.utn.frba.ddsi.logistica.services;

import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GestorPlanificacionRutas {

    private static final int TAMANO_LOTE = 100;

    private final AdapterPlanificadorExterno adapter;

    public GestorPlanificacionRutas(AdapterPlanificadorExterno adapter) {
        this.adapter = adapter;
    }

    @Async
    public void planificarEntregasDelDiaSiguiente(List<DonacionDTO> donacionesDisponibles, List<Camion> camiones) {
        System.out.println("LOGÍSTICA: Iniciando planificación del día siguiente...");

        if (donacionesDisponibles == null || donacionesDisponibles.isEmpty()) {
            return;
        }

        List<List<DonacionDTO>> lotes = this.fraccionarEnLotesDeCien(donacionesDisponibles);

        for (List<DonacionDTO> lote : lotes) {
            adapter.solicitarPlanificacionAsync(lote, camiones);
        }
    }

    private List<List<DonacionDTO>> fraccionarEnLotesDeCien(List<DonacionDTO> donaciones) {
        List<List<DonacionDTO>> lotes = new ArrayList<>();

        for (int i = 0; i < donaciones.size(); i += TAMANO_LOTE) {
            int fin = Math.min(i + TAMANO_LOTE, donaciones.size());
            lotes.add(new ArrayList<>(donaciones.subList(i, fin)));
        }

        return lotes;
    }
}
