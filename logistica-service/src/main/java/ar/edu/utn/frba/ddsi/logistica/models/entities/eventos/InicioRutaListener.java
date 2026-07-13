package ar.edu.utn.frba.ddsi.logistica.models.entities.eventos;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.notificacion.NotificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import jakarta.annotation.PostConstruct;

@Component
public class InicioRutaListener implements ListenerLogistica<EventoInicioRuta> {

    private final EventManagerLogistica eventManager;
    private final RestTemplate restTemplate;
    private final RestLogisticaConfig config;

    public InicioRutaListener(EventManagerLogistica eventManager, RestTemplate restTemplate, RestLogisticaConfig config) {
        this.eventManager = eventManager;
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @PostConstruct
    public void registerListeners() {
        eventManager.registrarListener(EventoInicioRuta.class, this);
    }

    @Override
    public void ejecutar(EventoInicioRuta ev) {
        procesarInicioRuta(ev);
    }

    private void procesarInicioRuta(EventoInicioRuta evento) {
        String mapaUrl = evento.getMapaUrl();

        for (Parada parada : evento.getRuta().getParadas()) {
            Long entidadId = parada.getEntidadId();
            try {
                String contactosEntidadUrl = UriComponentsBuilder.fromUriString(config.getDonacionesUrl())
                        .path("/entidad-beneficiaria/" + entidadId + "/contactos")
                        .build().toUriString();

                ResponseEntity<List<MedioContacto>> response = restTemplate.exchange(
                        contactosEntidadUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<MedioContacto>>() {
                        });

                List<MedioContacto> contactos = response.getBody();
                if (contactos != null) {
                    for (MedioContacto contacto : contactos) {
                        String mensaje = "Tu entrega ya está en camino. Seguí el recorrido del camión con el mapa interactivo: "
                                + mapaUrl;
                        enviarNotificacionAsync(contacto, mensaje);
                    }
                }
            } catch (Exception e) {
                System.err.println(
                        "Error al noticiar contacto de entidad ID: " + entidadId + ". Error: " + e.getMessage());
            }

            for (Long donacionId : parada.getDonacionIds()) {
                try {
                    String donacionUrl = UriComponentsBuilder.fromUriString(config.getDonacionesUrl())
                            .path("/donaciones/" + donacionId)
                            .build().toUriString();

                    DonacionResponse responseDonacion = restTemplate.getForObject(donacionUrl, DonacionResponse.class);
                    Long donanteId = responseDonacion.donanteId();

                    String contactoDonanteUrl = UriComponentsBuilder.fromUriString(config.getDonacionesUrl())
                            .path("/donantes/" + donanteId + "/contacto")
                            .build().toUriString();
                    MedioContacto contactoDonante = restTemplate.getForObject(
                            contactoDonanteUrl,
                            MedioContacto.class);

                    String mensaje = "Tu donación ya está en camino. Seguí el recorrido del camión con el mapa interactivo: "
                            + mapaUrl;
                    enviarNotificacionAsync(contactoDonante, mensaje);
                } catch (Exception e) {
                    System.err.println(
                            "Error al noticiar contacto para donación ID: " + donacionId + ". Error: "
                                    + e.getMessage());
                }
            }
        }
    }

    private void enviarNotificacionAsync(MedioContacto contacto, String mensaje) {
        NotificacionRequest request = new NotificacionRequest(
                contacto.getValor(),
                contacto.getTipoContacto(),
                mensaje);
        String notificacionesUrl = config.getNotificacionesUrl() + "/notificar";
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForObject(notificacionesUrl, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error enviando notificación asincrónica al microservicio de notificaciones: "
                        + e.getMessage());
            }
        });
    }
}
