package ar.edu.utn.frba.ddsi.logistica.services.impl;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.entities.GestorPlanificacionRutas;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;
import ar.edu.utn.frba.ddsi.logistica.services.PlanificacionRutasService;

@Service
public class PlanificacionRutasServiceImpl implements PlanificacionRutasService {

    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;
    private final GestorPlanificacionRutas gestorPlanificacionRutas;
    private final CamionRepository camionRepository;
    private static final int TAMANO_LOTE = 100;

    public PlanificacionRutasServiceImpl(RestTemplate restTemplate, RestLogisticaConfig properties,
            GestorPlanificacionRutas gestorPlanificacionRutas, CamionRepository camionRepository) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gestorPlanificacionRutas = gestorPlanificacionRutas;
        this.camionRepository = camionRepository;
    }

    public void planificarRutas() {
        List<DonacionDTO> lote = getLote();
        List<Camion> camionesDisponibles = camionRepository.findAllDisponibles();

        if (!lote.isEmpty() && !camionesDisponibles.isEmpty()) {
            gestorPlanificacionRutas.solicitarPlanificacion(lote, camionesDisponibles);
        }
    }

    public void ejecutarPlanificacion(EjecutarPlanificacionRequest request) {
        List<Long> donacionesPlanificadas = toListIdDonaciones(request.camiones());

        URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                .path("/donaciones-service/donacion/lista-entrega")
                .build().toUri();

        restTemplate.postForEntity(url, donacionesPlanificadas, Void.class);

        planificarRutas();
    }

    private List<DonacionDTO> getLote() {
        URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                .path("/donaciones-service/donacion/asignadas")
                .queryParam("limit", TAMANO_LOTE)
                .build().toUri();

        ResponseEntity<List<DonacionDTO>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<List<DonacionDTO>>() {
                });

        return response.getBody() != null ? response.getBody() : List.of();
    }

    private List<Long> toListIdDonaciones(List<CamionPlanificacionRequest> camiones) {
        return camiones.stream()
                .flatMap(c -> c.direcciones().stream())
                .flatMap(d -> d.donacionesIds().stream())
                .collect(Collectors.toList());
    }
}