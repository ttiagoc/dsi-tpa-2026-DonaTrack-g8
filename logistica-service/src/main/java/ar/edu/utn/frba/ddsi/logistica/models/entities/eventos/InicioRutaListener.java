package ar.edu.utn.frba.ddsi.logistica.models.entities.eventos;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;
import ar.edu.utn.frba.ddsi.logistica.dto.donacion.DonacionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.notificacion.NotificacionRequest;
import ar.edu.utn.frba.ddsi.logistica.grpc.DonacionesGrpcClient;
import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.Parada;
import jakarta.annotation.PostConstruct;

@Component
public class InicioRutaListener implements ListenerLogistica<EventoInicioRuta> {

    private final EventManagerLogistica eventManager;
    private final DonacionesGrpcClient donacionesGrpcClient;
    private final RestTemplate restTemplate;
    private final RestLogisticaConfig config;

    public InicioRutaListener(EventManagerLogistica eventManager,
            DonacionesGrpcClient donacionesGrpcClient,
            RestTemplate restTemplate,
            RestLogisticaConfig config) {
        this.eventManager = eventManager;
        this.donacionesGrpcClient = donacionesGrpcClient;
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
                List<MedioContacto> contactos = donacionesGrpcClient.obtenerContactosEntidad(entidadId);
                if (contactos != null) {
                    for (MedioContacto contacto : contactos) {
                        String mensaje = "Tu entrega ya está en camino. Seguí el recorrido del camión con el mapa interactivo: "
                                + mapaUrl;
                        enviarNotificacionAsync(contacto, mensaje, "EN_CAMINO", "ENTIDAD", entidadId, null);
                    }
                }
            } catch (Exception e) {
                System.err.println(
                        "Error al notificar contacto de entidad ID: " + entidadId + ". Error: " + e.getMessage());
            }

            for (Long donacionId : parada.getDonacionIds()) {
                try {
                    DonacionResponse responseDonacion = donacionesGrpcClient.obtenerDonacion(donacionId);
                    if (responseDonacion != null && responseDonacion.donanteId() != null) {
                        Long donanteId = responseDonacion.donanteId();
                        MedioContacto contactoDonante = donacionesGrpcClient.obtenerContactoDonante(donanteId);
                        if (contactoDonante != null) {
                            String mensaje = "Tu donación #" + donacionId + " ya está en camino. Seguí el recorrido del camión con el mapa interactivo: "
                                    + mapaUrl;
                            enviarNotificacionAsync(contactoDonante, mensaje, "EN_CAMINO", "DONANTE", donanteId, donacionId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                            "Error al notificar contacto para donación ID: " + donacionId + ". Error: "
                                    + e.getMessage());
                }
            }
        }
    }

    private void enviarNotificacionAsync(MedioContacto contacto, String mensaje, String tipoEvento, String tipoDestinatario, Long destinatarioId, Long donacionId) {
        NotificacionRequest request = new NotificacionRequest(
                contacto.getValor(),
                contacto.getTipoContacto(),
                mensaje,
                tipoEvento,
                tipoDestinatario,
                destinatarioId,
                donacionId);
        String notificacionesUrl = config.getNotificacionesUrl() + "/notificaciones";
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
