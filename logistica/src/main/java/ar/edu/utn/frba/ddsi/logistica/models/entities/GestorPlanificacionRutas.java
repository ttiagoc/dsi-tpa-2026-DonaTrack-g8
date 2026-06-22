package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;

public class GestorPlanificacionRutas {

    private static final int TAMANO_LOTE = 100;

    public List<List<DonacionDTO>> obtenerLotesParaPlanificar(List<DonacionDTO> donacionesDisponibles) {
        System.out.println("LOGÍSTICA: Iniciando planificación del día siguiente...");

        if (donacionesDisponibles == null || donacionesDisponibles.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<DonacionDTO>> lotes = this.fraccionarEnLotesDeCien(donacionesDisponibles);

        return lotes;
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
