package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.EstadoDonacionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.entregadonaciones.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventManagerLogistica;
import ar.edu.utn.frba.ddsi.logistica.models.entities.eventos.EventoInicioRuta;

@Component
public class GestorDeRutas {

    private final RestTemplate restTemplate;
    private final RestLogisticaConfig properties;
    private final EventManagerLogistica eventManager;

    public GestorDeRutas(RestTemplate restTemplate,
            RestLogisticaConfig properties, EventManagerLogistica eventManager) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.eventManager = eventManager;
    }

    public void iniciarRuta(Ruta ruta) {
        for (Parada parada : ruta.getParadas()) {
            for (Long donacionId : parada.getDonacionIds()) {
                URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                        .path("/donaciones/" + donacionId + "/estado")
                        .build().toUri();
                try {
                    EstadoDonacionRequest requestBody = new EstadoDonacionRequest(
                            "EN_TRASLADO",
                            "La donación se encuentra en camino a su destino.");
                    restTemplate.put(url, requestBody);
                } catch (Exception e) {
                    System.err
                            .println("ERROR: Falló la notificación de inicio de ruta para donación ID: " + donacionId);
                }
            }
        }

        eventManager.emitir(
                new EventoInicioRuta(ruta, properties.getLogisticaUrl() + "/monitoreo/ubicacion/" + ruta.getId()));
    }

    public void confirmarEntregaExitosa(Parada paradaAfectada, Ruta ruta) {
        ConfirmacionEntregaExitosaRequest request = mapToConfirmacionEntregaExitosaRequest(paradaAfectada, ruta);
        URI url = UriComponentsBuilder.fromUriString(properties.getDonacionesUrl())
                .path("/donaciones/recepciones")
                .build().toUri();
        try {
            restTemplate.postForObject(url, request, Void.class);
            System.out.println("Reporte de entrega exitosa enviado a Donaciones para Parada ID: " + paradaAfectada.getOrden());
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo transmitir la confirmación de entrega por red.");
        }
    }

    private ConfirmacionEntregaExitosaRequest mapToConfirmacionEntregaExitosaRequest(Parada paradaAfectada, Ruta ruta) {
        return new ConfirmacionEntregaExitosaRequest(
                paradaAfectada.getEntidadId(), paradaAfectada.getDonacionIds(),
                ruta.getCamion().getPatente(), LocalDateTime.now());
    }
}
