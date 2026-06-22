package ar.edu.utn.frba.ddsi.logistica.models.entities;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.ResultadoPlanificacionDTO;

public class ReceptorPlanificacionRutas {
    public void recibirPlanificacion(ResultadoPlanificacionDTO resultado) {
        System.out.println("LOGÍSTICA: Procesando el veredicto del planificador externo...");

        if (resultado == null) {
            return;
        }

        if (resultado.getRutasAsignadas() != null) {
            for (Ruta ruta : resultado.getRutasAsignadas()) {
                System.out.println("LOGÍSTICA: Ruta ID #" + ruta.getId() + " guardada en el sistema.");
            }
        }

        if (resultado.getDonacionesSinAsignar() != null && !resultado.getDonacionesSinAsignar().isEmpty()) {
            this.procesarDonacionesRechazadas(resultado.getDonacionesSinAsignar());
        }
    }

    private List<DonacionDTO> procesarDonacionesRechazadas(List<DonacionDTO> donacionesSobrantes) {
        System.err.println(
                "LOGÍSTICA: Procesando " + donacionesSobrantes.size() + " donaciones que se quedaron sin camión.");

        for (DonacionDTO donacion : donacionesSobrantes) {
            donacion.setEstado(TipoEstadoDonacion.LISTA_PARA_ENTREGAR);
        }

        return donacionesSobrantes;
    }
}