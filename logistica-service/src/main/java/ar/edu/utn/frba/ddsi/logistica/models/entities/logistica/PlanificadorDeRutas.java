package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionDTO;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.CamionPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.planificacion.EjecutarPlanificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@Component
public class PlanificadorDeRutas {

    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;
    private final GestorPlanificacionRutas gestorPlanificacionRutas;
    private final CamionRepository camionRepository;
    private static final int TAMANO_LOTE_DONACIONES = 100;
    private static final int TAMANO_LOTE_CAMIONES = 5;

    public PlanificadorDeRutas(RestTemplate restTemplate, RestLogisticaConfig properties,
            GestorPlanificacionRutas gestorPlanificacionRutas, CamionRepository camionRepository) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gestorPlanificacionRutas = gestorPlanificacionRutas;
        this.camionRepository = camionRepository;
    }

    public void planificarRutas() {
        List<Camion> camionesDisponibles = camionRepository.findAllDisponibles();
        if (camionesDisponibles.isEmpty()) {
            return;
        }

        int offset = 0;
        int camionIndex = 0;

        while (true) {
            if (camionIndex >= camionesDisponibles.size()) {
                break;
            }

            List<DonacionDTO> loteDonaciones = getLote(offset);
            if (loteDonaciones.isEmpty()) {
                break;
            }

            int toIndex = Math.min(camionIndex + TAMANO_LOTE_CAMIONES, camionesDisponibles.size());
            List<Camion> loteCamiones = camionesDisponibles.subList(camionIndex, toIndex);

            gestorPlanificacionRutas.solicitarPlanificacion(loteDonaciones, loteCamiones);

            offset += TAMANO_LOTE_DONACIONES;
            camionIndex += TAMANO_LOTE_CAMIONES;
        }
    }

    public void ejecutarPlanificacion(EjecutarPlanificacionRequest request) {
        List<Long> donacionesPlanificadas = toListIdDonaciones(request.camiones());

        for (Long donacionId : donacionesPlanificadas) {
            URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                    .path("/donaciones/" + donacionId + "/estado")
                    .build().toUri();

            EstadoDonacionRequest requestBody = new EstadoDonacionRequest(
                    "LISTA_PARA_ENTREGAR",
                    "Donacion lista para entregar");

            restTemplate.put(url, requestBody);
        }
    }

    private List<DonacionDTO> getLote(int offset) {
        URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                .path("/donaciones/estado/asignacion_realizada")
                .queryParam("limit", TAMANO_LOTE_DONACIONES)
                .queryParam("offset", offset)
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
