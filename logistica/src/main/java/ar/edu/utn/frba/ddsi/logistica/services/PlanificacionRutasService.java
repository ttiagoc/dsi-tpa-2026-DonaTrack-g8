package ar.edu.utn.frba.ddsi.logistica.services;

import java.net.URI;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.GestorPlanificacionRutas;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@Service
public class PlanificacionRutasService {

    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;
    private final GestorPlanificacionRutas gestorPlanificacionRutas;
    private final CamionRepository camionRepository;
    private final int TAMANO_LOTE = 100;

    public PlanificacionRutasService(RestTemplate restTemplate, RestLogisticaConfig properties,
            GestorPlanificacionRutas gestorPlanificacionRutas, CamionRepository camionRepository) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gestorPlanificacionRutas = gestorPlanificacionRutas;
        this.camionRepository = camionRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void planificarRutas() {
        List<DonacionDTO> donacionesAsignadas = getDonacionesAsignadas();
        List<Camion> camionesDisponibles = camionRepository.findAll();

        if (donacionesAsignadas.isEmpty() && camionesDisponibles.isEmpty()) {
            gestorPlanificacionRutas.solicitarPlanificacion(lote, camionesDisponibles);
        }
    }

    public void ejecutarPlanificacion() {

        List<DonacionDTO> donacionesAsignadas = getDonacionesAsignadas();
        List<DonacionDTO> lote = donacionesAsignadas.subList(0, TAMANO_LOTE);
        List<Camion> camionesDisponibles = camionRepository.findAll();

        if (donacionesAsignadas.isEmpty() && camionesDisponibles.isEmpty()) {
            gestorPlanificacionRutas.solicitarPlanificacion(lote, camionesDisponibles);
        }
    }

    private List<DonacionDTO> getDonacionesAsignadas() {
        URI url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/donaciones/asignadas")
                .queryParam("limit", 100)
                .build().toUri();
        try {
            return restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            System.err.println("ERROR: Falló la consulta a Donaciones.");
            return null;
        }
    }
}
