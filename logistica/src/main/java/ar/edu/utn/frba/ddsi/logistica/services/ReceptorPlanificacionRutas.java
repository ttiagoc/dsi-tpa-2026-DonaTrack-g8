package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.common.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.ResultadoPlanificacionDTO;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Ruta;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.RutaRepository;

@Service
public class ReceptorPlanificacionRutas {

    private final RutaRepository rutaRepository;
    private final DonacionesLogsticaService donacionesLogsticaService;

    public ReceptorPlanificacionRutas(RutaRepository rutaRepository,
            DonacionesLogsticaService donacionesLogsticaService) {
        this.rutaRepository = rutaRepository;
        this.donacionesLogsticaService = donacionesLogsticaService;
    }

    public void recibirPlanificacion(ResultadoPlanificacionDTO resultado) {
        System.out.println("LOGÍSTICA: Procesando el veredicto del planificador externo...");

        if (resultado == null) {
            return;
        }

        if (resultado.getRutasAsignadas() != null) {
            for (Ruta ruta : resultado.getRutasAsignadas()) {
                rutaRepository.save(ruta);
                System.out.println("LOGÍSTICA: Ruta ID #" + ruta.getId() + " guardada en el sistema.");
            }
        }

        if (resultado.getDonacionesSinAsignar() != null && !resultado.getDonacionesSinAsignar().isEmpty()) {
            this.procesarDonacionesRechazadas(resultado.getDonacionesSinAsignar());
        }
    }

    private void procesarDonacionesRechazadas(List<DonacionDTO> donacionesSobrantes) {
        System.err.println(
                "LOGÍSTICA: Procesando " + donacionesSobrantes.size() + " donaciones que se quedaron sin camión.");

        for (DonacionDTO donacion : donacionesSobrantes) {
            donacion.setEstado(TipoEstadoDonacion.LISTA_PARA_ENTREGAR);
        }

        this.donacionesLogsticaService
                .notificarDonacionesRechazadas(donacionesSobrantes.stream().map(DonacionDTO::getId).toList());
    }
}